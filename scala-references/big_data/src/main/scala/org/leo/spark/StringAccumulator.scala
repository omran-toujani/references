package org.leo.spark

import org.apache.spark.util.AccumulatorV2

/**
  * Custom Accumulator for String objects
  * the generic types are for input, output values, and they can be different
  */
class StringAccumulator extends AccumulatorV2[String, String] {

  var stringVal: String = ""

  override def isZero: Boolean = stringVal.length <= 0

  override def copy(): AccumulatorV2[String, String] = {
    val x = new StringAccumulator()
    x.stringVal = this.stringVal
    x
  }

  override def reset(): Unit = stringVal = ""

  override def add(v: String): Unit = stringVal = stringVal.concat(v)

  override def merge(other: AccumulatorV2[String, String]): Unit = stringVal = stringVal.concat(other.value)

  override def value: String = stringVal
}
