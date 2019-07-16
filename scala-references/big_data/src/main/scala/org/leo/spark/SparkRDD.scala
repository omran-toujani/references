package src.main.scala.org.leo.spark

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.io.{IntWritable, LongWritable, Text}
import org.apache.hadoop.mapred.{InputFormat, JobConf}
import org.apache.hadoop.mapreduce.{InputFormat => NewInputFormat}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}
import org.leo.spark.StringAccumulator

import scala.reflect._

/**
  * In this object we try to demonstrate the old RDD APi, even if it is still available through
  * the SparkSession component, we will use the SparkContext in this class to stick with old APIs
  */
object SparkRDD extends App {

  // we always start by linking to spark using a SparkConf object, for tests we use the local mode
  val sparkConf = new SparkConf().setAppName("testingSparkRDD").setMaster("local")

  // next thing we do to finish linking to spark is creating a SparkContext
  val sc = new SparkContext(sparkConf)

  // let's create an RDD by parallelizing some driver data
  val data = Array(1, 2, 3, 4, 5)
  val dataRDD = sc.parallelize(data, 4)   // the second argument is optional and is used to set the number of partitions
                                          // we want to create in the resulting RDD

  /**
    * Spark can connect to pretty much any fucking place were you can store data, it supports for example Local files,
    * HDFS, S3, HBase, Cassandra, Elasticsearch, Kafka, relational databases through JDBC
    * For files Spark can read also a large variety of them, ranging from simple text files to Sequence files going
    * through any HDFS InputFormat
    * We will talk about each of these sources (each one in its own object) but here are some example of methods
    * to create RDDs from files using the SparkContext object :
    */

  /**
    * When creating an RDD from a local file, the latter should be available in all workers in the same path
    * Also, all file based methods creating RDDs support directories, compressed files and wildcards "*"
    */

  /**
    * RDD from a text file (local, HDFS or S3 or any valid URI for some file somewhere), it creates an RDD with
    * elements being a record for each line
    */
  val fileRDD = sc.textFile("local-file.txt", 2)    // the second argument is optional and asks spark to create at least
                                                    // 2 partitions, by default it creates as many partitions as HDFS
                                                    // blocks(128MB by default), the argument will only work if it is
                                                    // higher than the default minimum
  /**
    * Unlike textFile, the wholeTextFiles method create a pair RDD with file names as keys and the entire file content
    * as values, it also takes the same optional minimum partition number argument
    */
  val wholeTextFilesRDD = sc.wholeTextFiles("someDirectory", 100)

  /**
    * For sequence files the sequenceFile method take also a minimum partition optional argument
    * the key and value classes need to be implementations of Hadoop's Writable interface even if some
    * primitive types ara allowed like String or scala's Int
    */
  val sequenceFileRDD = sc.sequenceFile("some_sequence_file_path", classOf[IntWritable], classOf[Text], 50);

  /**
    * For any other hadoop InputFormat, or for a custom InputFormat we can use the hadoopFile and hadoopRDD
    * methods or their newer version newAPIHadoopFile and newAPIHadoopRDD
    * the main difference between the two pairs is the hadoop api used, newAPI means the one with mapreduce instead of
    * mapred
    * as for the difference between the two methods of the same pair is that the first (hadoopFile or newAPIHadoopFile)
    * takes the path of the file to read while the two others (hadoopRDD and newAPIHadoopRDD) takes a hadoop
    * configuration object
    */

  // Old Hadoop API methods :
  // this one takes the files to read as a path argument
  val hadoopFileRDD = sc.hadoopFile(
    "some_file_path",                      // hadoop file path
    classOf[InputFormat[IntWritable, Text]],      // any implementation from hadoop.mapred.InputFormat
    classOf[IntWritable],                  // key class for result RDD
    classOf[Text],                         // value class for result RDD
    100,
  )

  // this one takes the files to read from a JobConf object, this is actually used to read
  // hbase tables for example (by passing a HbaseConfiguration which extends the JobConf)
  // the job conf actually defines where and how to get the data
  val hadoopRDD = sc.hadoopRDD(
    new JobConf(),                       // the one from mapred or any extending classes like HbaseConfiguration
    classOf[InputFormat[LongWritable, Text]],    // any implementation from hadoop.mapred.InputFormat, TableInputFormat for Hbase
    classOf[LongWritable],               // key class for result RDD
    classOf[Text],                       // value class for result RDD
    100                                  // the usual stupid most of the time useless minimal parition number
  )

  // New Hadoop API methods : almost the same as the old but using new mapreduce api and without optional partition arg
  val newAPIHadoopFileRDD = sc.newAPIHadoopFile(
    "some_file_path",             // hadoop file path
    classOf[NewInputFormat[Text, Text]],   // any implementation from hadoop.mapreduce.InputFormat
    classOf[Text],               // key class for result RDD
    classOf[Text]                       // value class for result RDD
  )

  val newAPIHadoopRDD = sc.newAPIHadoopRDD(
    new Configuration(),                 // hadoop.conf.Configuration
    classOf[NewInputFormat[LongWritable, Text]],    // any implementation from hadoop.mapreduce.InputFormat, TableInputFormat for Hbase
    classOf[LongWritable],               // key class for result RDD
    classOf[Text]                        // value class for result RDD
  )

  /**
    * The spark context also can read already saved RDDs
    * we can save an RDD as a serialized object then read it with spark context :
    */
  fileRDD.saveAsObjectFile("some_path_to_store_rdd")              // saving the RDD as serialized object
  val restoredRDD = sc.objectFile("some_path_to_store_rdd", 100)  // again the second arg is optional

  /**
    * An RDD can have two types of operations: transformations and actions
    * - transformations are lazy operations that create new RDDs from existing ones, they do not execute until an action
    * is called
    * - actions are operations that return computation values to the driver program
    * normally, each time an action is called upon a transformed RDD, the latter is recomputed, unless we call
    * cache or persist on it before the action call causing it to be stored as a transformation result and not being
    * computed each time
    */

  /**
    * Passing methods to RDD operations : there are two recommended ways
    */

  // for short code write it as an anonymous function
  fileRDD.map(x => println)   // no need to say that this print will print all elements if we are in local mode to the
                              // stdout of the driver because it is the worker in this case,
                              // but in distributed mode each worker will print its own stdout

  // for more large code a static method from a global singleton object
  fileRDD.map(FunctionsProvider.doPrinting)

  // we can of course reference methods of classes instances (this or another class instance), but this would send the
  // object to spark's worker nodes
  def doStuff(s: String) = println
  fileRDD.map(this.doStuff)

  // when referencing a member of a given class (this or any other) we also send the whole object to the cluster
  // a good way to avoid this is to copy the value inside the operation
  val text = "hello"
  val num = 0

  fileRDD.map(x => {
     // x + text   // this sends the whole object
     // num++      // also and btw, this will not change the num, do not try to modify drivers attributes from worker
                   // closures, it has no effect on it and even if it may work sometimes in local mode it still
                   // pointless

    val copy = text
    x + copy      // this is better
  })

  /**
    * Key value RDDs, known as PairRDDs in java spark API, are just RDDs of Tuple2 in scala
    * So operations that work on pair rdd are automatically available on RDDs with tuple objects
    * Another thing about key value RDDs is that if you use custom objects as keys, just like a Map
    * you need to make sure to enforce the contract over the hashCode and the equals methods
    */
  val pairRDD = fileRDD.map(x => (x, 1))
  pairRDD.reduceByKey((a, b) => a + b)

  /**
    * Some of the main RDDs transformations, we will not test all of them, only some common ones
    * so you will need to check the Scala Doc if you want to know 'em all
    */
  val rdd1 = fileRDD.map(s => s + ".")              // returns a new RDD with the same size of the base RDD
  val rdd2 = fileRDD.flatMap(s => Seq(s, s))        // returns 0 or more values for each element

  val rdd3 = fileRDD.mapPartitions(p => {           // same as map but works on an entire partition
    p.map(s => s + ".")
  })

  val rdd4 = fileRDD.mapPartitionsWithIndex((index, p) => { // same as mapPartitions but involves the partition index
    p.map(s => s + index)                                   // as the functions parameter
  })

  val rdd5 = fileRDD.filter(s => s.length > 1)     // returns a filtered rdd given the predicate function
  val rdd6 = fileRDD.sample(false, .3, 1)          // returns a sample of the rdd with the fraction size using a seed
                                                   // number fo random number generator and boolean to say if elements
                                                   // can be sampled multiple times or need to be replaced

  val union = fileRDD.union(rdd1)                  // union of two RDDs
  val inter = fileRDD.intersection(rdd1)           // intersection of two RDDs (we can specify partitions or partitioner)
  val dist = fileRDD.distinct()                    // distinct elements of the same RDD, we can specify the number
                                                   // of partitions and the Ordering object
  val pair1 = pairRDD.groupByKey()                 // returns pair RDD with unique keys and an iterable as value
  val pair2 = pairRDD.reduceByKey((a, b) => a + b) // returns pair RDD with unique keys and reduced value

  val pair3 = pairRDD.aggregateByKey((0,0))(                // return a pair RDD with unique keys and potentially
    (acc, value) =>  (acc._1 + 1, acc._2 + value),          // values with different type than the original
    (acc1, acc2) => (acc1._1 + acc2._1, acc1._2 + acc2._2)  // it is used to make aggregation when needing accumulators
  )

  val pair4 = pairRDD.sortByKey(true)             // returns a pair RDD sorted by keys, the boolean is for ascending
                                                  // the key class need to extend the Ordered Trait
                                                  // RQ : Ordered is to scala what Comparable is to Java
                                                  // and Ordering is to scala what Comparator is to Java

  val pair5 = pairRDD.join(pairRDD)               // exactly an SQL inner join on the key values, returns a pair RDD with all
                                                  // keys and values as tuples of all possible pair between the 2 RDD
                                                  // other joins are possible using fullOuterJoin, leftOuterJoin and
                                                  // rightOuterJoin

  val pair6 = pairRDD.cogroup(pairRDD)            // returns an RDD with values being tuples of iterables of both rdds
                                                  // values for each key

  val pair7 = fileRDD.cartesian(dataRDD)          // returns a pair RDD with cartesian product (all the pairs) of
                                                  // the two RDDs

  val rdd7 = dataRDD.pipe("some_script.sh")       // calls a script on the RDD, it can be shell, python, perl, R or any
                                                  // other shitty script

  /**
    * Let's now see some main spark RDD actions, here again it is not exhaustive, just for test
    */
  val sum = dataRDD.reduce((x, y) => x + y)         // No need to explain i think it's clear enough
  val sample = dataRDD.take(2)                      // returns array of the first data from the RDD with the given size

  dataRDD.foreach(println)                          // one of the most important actions, it runs a function for each
                                                    // element of the RDD, it is mostly used for side effects or to
                                                    // interact with other data sources, you may prefer the
                                                    // foreachPartition variant that runs the function for each partition

  /**
    * All operations that involve repartition, a byKey operation, joins, sorts, groupings and any other operation
    * that needs to move data somehow starts what we call shuffle, which is a very expensive and costly operation
    */

  /**
    * RDD Persistence : One of the key features of Spark is the ability to store an RDD in memory in the first time it
    * is computed (with an action), this will cause the RDD to be cached (each partition in its worker node)
    * this will make the next operations on the same RDD run faster
    */
  dataRDD.cache                           // we could use persist instead, cache is persist with storage mode being
                                          // memory only

  // other data storage modes are possible :
  dataRDD.persist(StorageLevel.MEMORY_ONLY)       // default one, deserialized java object saved to JVM, if does not
                                                  // fit they are partially cached and the rest not is not cached, this
                                                  // is the best CPU efficient mode
  dataRDD.persist(StorageLevel.DISK_ONLY)         // deserialized java objects saved on disk
  dataRDD.persist(StorageLevel.MEMORY_AND_DISK)   // same as MEMORY_ONLY but the rest if unfit is stored to disk
  dataRDD.persist(StorageLevel.MEMORY_ONLY_SER)   // serialized java objects stored in memory, more space efficient
                                                  // but more costly on CPU usage

  // Other variants of storage level include a combination of MEMORY, DISK, SER and 2, the two means that each partition
  // is replicated in two nodes

  /**
    * There is one more storage level that is still in the experimental phase as when this is written, it is called
    * OFF HEAP and is more like MEMORY_ONLY_SER but uses a cache system such as Tachyon as a off heap store
    */
  dataRDD.persist(StorageLevel.OFF_HEAP)

  // to remove an RDD from the cache :
  dataRDD.unpersist()

  /**
    * One more thing about caching, use the default one when you can, then if the data does not fit in memory use
    * the SER storage (serialized data are smaller), don't use storage that spill to disk unless you have no choice
    * and don't use the replication option too much because spark already have a fault tolerance by rebuilding failed
    * partitions but if you need not to wait the time it rebuilds them then use that replication storage
    *
    * Note that for python users, all storage levels are the same one, RDDs will always be serialized using the Pickle
    * Library, and there will still some people who use this fucking inferior language (personal opinion...)
    */

  /**
    * Shared variables : Broadcasts and Accumulators
    */

  /**
    * Broadcasting a variable is a way to share it in read only mode between all partitions on all worker nodes, it is useful for large
    * shared data, and should be used carefully, for example you should refer to the broadcast variable and not the
    * original one in you program, and you should not change the original value in the program so the broadcast
    * variable is coherent over stages (unless you know what you are doing of course)
    */
  val broadcastVal = sc.broadcast(Array(1, 2, 3))

  dataRDD.foreach(x => println(broadcastVal.value))          // this is how we access the broadcast value

  /**
    * Accumulators in the other hand are write only shared variables that can only be added
    * They are usually used for sums and counters
    * Spark comes with only numeric accumulators, a Long and a Double ones but you can write your own ones
    * RQ: now also collection accumulators are natively added to spark along with double and long
    * In actions accumulators are guaranteed to be executed only once per task (per partition) even if the
    * task restarts after a failure, this is not the case inside a transformation
    */
  val longAcc = sc.longAccumulator("longAcc")                         // registering a natively supported accumulator

  val stringAcc = new StringAccumulator
  sc.register(stringAcc, "stringAcc")                                 // registering a custom accumulator, beware
                                                                      // of the difference  !!!
  dataRDD.foreach(y => {
    longAcc.add(y)
    stringAcc.add(y.toString)
  })

}

// Just for tests sake
object FunctionsProvider {
  def doPrinting(s: String) = println
}
