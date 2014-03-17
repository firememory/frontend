/**
 * Copyright (C) 2014 Coinport Inc.
 * Author: Chunming Liu (chunming@coinport.com)
 */

package models

object CurrencyUnit extends Enumeration {
  type CurrencyUnit = Value
  val NO_UNIT, BTC, MBTC, CNY, CNY2 = Value

  val factors: Map[(CurrencyUnit, CurrencyUnit), Double] = Map(
    (BTC, MBTC) -> 1000.0,
    (MBTC, BTC) -> 0.001,
    (CNY, CNY2) -> 100,
    (CNY2, CNY) -> 0.01
  )
}

import CurrencyUnit._

object CurrencyValue {
  implicit def long2CurrencyUnit(value: Long) = new CurrencyValue(value)
  implicit def double2CurrencyUnit(value: Double) = new CurrencyValue(value)
  implicit def currencyUnit2Long(value: CurrencyValue) = value.toLong
  implicit def currencyUnit2Double(value: CurrencyValue) = value.toDouble

  implicit def double2PriceUnit(value: Double): PriceValue = new PriceValue(value)
  implicit def priceUnit2Double(value: PriceValue) = value
}

case class CurrencyValue(value: Double) {
  private var currencyUnit: CurrencyUnit = _

  def unit(unit: CurrencyUnit): CurrencyValue = {
    this.currencyUnit = unit
    this
  }

  def unit: CurrencyUnit = currencyUnit

  def / (operand: CurrencyValue): PriceValue = {
    val units = (currencyUnit, operand.currencyUnit)
    if (units._1 == units._2) { PriceValue(value / operand.value) }
    else {
      factors.get(units) match {
        case Some(factor) => // same family of currency
          PriceValue(value * factor / operand.value)
        case None => // price
          PriceValue(value / operand.value, units)
      }
    }
  }

  def to (newUnit: CurrencyUnit): CurrencyValue = {
    println("convert from " + currencyUnit + " to " + newUnit)
    if (currencyUnit == newUnit) { this }
    else {
      factors.get((currencyUnit, newUnit)) match {
        case Some(factor) => CurrencyValue(value * factor).unit(newUnit)
        case None => this // can't convert between different currencies
      }
    }
  }

  def toLong = value.toLong
  def toDouble = value

  override def toString: String = {
    "[" + value + " " + currencyUnit + "]"
  }

  override def equals(obj: scala.Any): Boolean = {
    if (obj.isInstanceOf[CurrencyValue]) {
      val other = obj.asInstanceOf[CurrencyValue]
      val me = this to other.currencyUnit
      me.value == other.value && me.unit == other.unit
    } else {
      false
    }
  }
}

case class PriceValue(value: Double, unit: (CurrencyUnit, CurrencyUnit) = (NO_UNIT, NO_UNIT)) {

  def inverse = PriceValue(1 / value, (unit._2, unit._1))

  override def equals(obj: scala.Any): Boolean = {
    if (obj.isInstanceOf[PriceValue]) {
      val other = obj.asInstanceOf[PriceValue]
      val result = this.value == other.value && this.unit == other.unit
      println(this + " compare with " + obj + " = " + result)
      result
    } else {
      false
    }
  }

  override def toString: String = {
    "[" + value + " " + unit._1 + "/" + unit._2 + "]"
  }
}