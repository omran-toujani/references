package org.leo.spark

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.{Encoders, SQLContext, SparkSession}

/**
  * Here we are going to test the Spark SQL API providing the DataFrame and Dataset APIs along with
  * the other SQL related capabilities
  */
object SparkSQL extends App {

  // until Spark 1.6, the entry point to Spark SQL capabilities was the SQLContext created like this :
  val sparkConf = new SparkConf().setAppName("testingRDD").setMaster("local")
  val sc = new SparkContext(sparkConf)
  val sqlContext = new SQLContext(sc)

  // tarting from Spark 2, the SQLContext and the SparkContext are merged in the SparkSession object
  val session = SparkSession
    .builder()
    .appName("testingSparkSQl")
    .master("local")
    .config("some key", "some value")       // key values config or even a SparkConf object
    .enableHiveSupport()                    // no longer need to create HiveContext, just use this on the session
    .getOrCreate()

  // Spark Session have methods that can return a SparkContext or a SQLContext object
  val sc2 = session.sparkContext
  val sqlContext2 = session.sqlContext

  // we can create multiple sessions in spark to get different sql catalogs
  val session2 = session.newSession()

  // DataFrames :
  /**
    * Unlike RDD, data in a DataFrame are organized in named columns like an SQL table
    */

  /**
    * the read method returns a DataFrameReader object, which has a lot of methods to create an RDD from pretty much
    * any thing (csv, text, parquet, jdbc, orc, json
    */
  val jsonDf = session.read.json("/path/sample.json")

  /**
    * Operations on DataFrames are called untyped operations in contrast with Dataset operations that are typed
    * Actually an RDD is a Dataset of Row
    */
  jsonDf.select("column_1")       // return DF with the given column

  // suppose the json has names and ages
  jsonDf.select(jsonDf("name"), jsonDf("age)" + 1))    // the dfName("col") notation is an apply that returns the column
                                                       // in java its counterpart is dfName.col

  import session.implicits._                          // implicit to use the $ notation (in java we can) use static import
                                                      // import static org.apache.spark.sql.functions.col

  jsonDf.select($"age" > 20)                          // the $ is implicit for dfName("col") in java it's col("col")

  // DataFrames have a lot of methods to manipulate, aggregate et compute around columns and expressions

  // we can register a DF as a temporary view to run sql queries on it
  jsonDf.createOrReplaceTempView("people")

  // running sql returns a DataSet of Rows that we can transform to a DataFrale
  val people = session.sql("Select * from people").toDF()

  // DFs regiistered as temporary views are only accessible in the sessions scope, we can register a DF as a global
  // temporary view so it is available in all sessions of a spark application via the global_temp database
  people.createOrReplaceGlobalTempView("sharedPeople")

  val sharedPeople = session2.sql("Select * from global_temp.sharedPeople")

  // Datasets :
  /**
    * Datasets are a lot like RDDs, however, instead of using Java serialization or Kryo they use a specialized
    * Encoder to serialize the objects (Elements) for processing or transmitting over the network.
    * While both encoders and standard serialization are responsible for turning an object into bytes, encoders are
    * code generated dynamically and use a format that allows Spark to perform many operations like filtering,
    * sorting and hashing without deserializing the bytes back into an object.
    * To understand the difference between RDD serialization using core java or Kryo and Encoder based serialization
    * one need to understand how off-heap memory works, in fact encoders works as a dynamic interface between the
    * data stored in off-heap memory in binary format, they generate code dynamically to access some of the data
    * attributes without deserializing the entire stored objects
    *
    * Of course Datasets does not exist in the world of the so good dynamically typed Python...
    */

  // creating Datasets

  // Encoders are created automatically for case classes
  // for java you need a bean class and you need to create an Encoder using Encoders.bean, then you need to pass
  // the data and the encoder to the createDataSet method of SparkSession
  case class Person(name: String, age: Int)

  val personsDs = Seq(Person("Bob", 20), Person("John", 45)).toDS()     // the Seq.toDS is done with implicit magic

  // Encoders for common types are already provided using implicits
  // In Java you need to pass the data and Encoders.Type(INT...) to the createDataset method of SparkSession
  // we can even tell spark to use Kryo or simple java serialization as Encoder
  val intDs = Seq(1, 2, 3).toDS()

  // A DataFrame can be transformed to a Dataset given a mapping class (the mapping is done by name between the class
  // attributes and the DF column names
  val jsonDs = jsonDf.as[Person]



  //HiveContext and Hive Support

  // JDBC SQL
}
