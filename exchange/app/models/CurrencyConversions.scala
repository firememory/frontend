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
    (CNY, CNY2) -> 100.0,
    (CNY2, CNY) -> 0.01
  ) ++ CurrencyUnit.values.map(u => (u, u) -> 1.0).toMap

  // get user-friendly unit
  def userUnit(unit: CurrencyUnit): CurrencyUnit = {
    unit match {
      case CNY2 => CNY
      case MBTC => BTC
      case _ => unit
    }
  }

  // get inner unit
  def innerUnit(unit: CurrencyUnit): CurrencyUnit = {
    unit match {
      case CNY => CNY2
      case BTC => MBTC
      case _ => unit
    }
  }
}

import CurrencyUnit._
import com.coinport.coinex.data.Currency
import com.coinport.coinex.data.Currency.Btc
import com.coinport.coinex.data.Currency.Rmb

object CurrencyValue {
  implicit def long2CurrencyUnit(value: Long) = new CurrencyValue(value)
  implicit def double2CurrencyUnit(value: Double) = new CurrencyValue(value)
  implicit def currencyUnit2Long(value: CurrencyValue) = value.toLong
  implicit def currencyUnit2Double(value: CurrencyValue) = value.toDouble

  implicit def double2PriceUnit(value: Double): PriceValue = new PriceValue(value)
  implicit def priceUnit2Double(value: PriceValue) = value

  implicit def currency2CurrencyUnit(value: Currency): CurrencyUnit = {
    value match {
      case Btc => MBTC
      case Rmb => CNY2
      case _ => NO_UNIT
    }
  }

  implicit def currencyUnit2Currency(value: CurrencyUnit): Currency = {
    value match {
      case BTC => Btc
      case MBTC => Btc
      case CNY => Rmb
      case CNY2 => Rmb
    }
  }
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

  def * (operand: PriceValue): CurrencyValue = {
    if (operand.unit == (NO_UNIT, NO_UNIT)) {
      CurrencyValue(value * operand.value).unit(unit)
    } else {
      factors.get(this.unit, operand.unit._2) match {
        case Some(factor) => CurrencyValue(value * operand.value * factor).unit(operand.unit._1)
        case None => this // can't multiply
      }
    }
  }

  def to (newUnit: CurrencyUnit): CurrencyValue = {
//    println("convert from " + currencyUnit + " to " + newUnit)
    if (currencyUnit == newUnit) { this }
    else {
      factors.get((currencyUnit, newUnit)) match {
        case Some(factor) => CurrencyValue(value * factor).unit(newUnit)
        case None => this // can't convert between different currencies
      }
    }
  }

  def userValue: Double = {
    to(userUnit(currencyUnit)).value
  }

  def innerValue: Double = {
    to(innerUnit(currencyUnit)).value.toLong
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

  def inverse = PriceValue(1.0 / value, (unit._2, unit._1))

  def unit(unit: (CurrencyUnit, CurrencyUnit)): PriceValue = {
    // TODO: avoid to create new object
    PriceValue(value, unit)
  }

  def *(operand: CurrencyValue): CurrencyValue = {
    if (unit == (NO_UNIT, NO_UNIT)) {
      operand
    } else {
      factors.get(operand.unit, this.unit._2) match {
        case Some(factor) => CurrencyValue(value * operand.value * factor).unit(this.unit._1)
        case None => operand // can't multiply
      }
    }
  }

  def to (newUnit: (CurrencyUnit, CurrencyUnit)): PriceValue = {
//    println("converting price: " + this + " to " + newUnit)
    if (newUnit.swap == unit)
      inverse
    else {
      factors.get(unit._1, newUnit._1) match {
        case Some(factor1) =>
          factors.get(unit._2, newUnit._2) match {
            case Some(factor2) =>
//              println("factors: " + factor1 + ", " + factor2 + "\nto " + PriceValue(value * factor1 / factor2, newUnit))
              PriceValue(value * factor1 / factor2, newUnit)
            case None => this
          }
        case None => this
      }
    }
  }

  def userValue: Double = {
    val newUnit: (CurrencyUnit, CurrencyUnit) = (userUnit(unit._1), userUnit(unit._2))
    to(newUnit).value
  }

  def innerValue: Double = {
    val newUnit: (CurrencyUnit, CurrencyUnit) = (innerUnit(unit._1), innerUnit(unit._2))
    to(newUnit).value.toDouble
  }

  override def equals(obj: scala.Any): Boolean = {
    if (obj.isInstanceOf[PriceValue]) {
      val other = obj.asInstanceOf[PriceValue]
      val result = this.value == other.value && this.unit == other.unit
      result
    } else {
      false
    }
  }

  override def toString: String = {
    "[" + value + " " + unit._1 + "/" + unit._2 + "]"
  }
}