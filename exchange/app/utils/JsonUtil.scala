///**
// * Copyright (C) 2014 Coinport Inc.
// * Author: Chunming Liu (chunming@coinport.com)
// */
//package utils
//
//import com.google.gson._
//import play.api.libs.json.{Json, JsValue}
//import java.lang.reflect.Type
//
//import spray.json._
//import DefaultJsonProtocol._
//
//class EnumSerializer extends JsonSerializer[Enumeration#Value] {
//  def serialize(obj: Enumeration#Value,  arg1: Type, arg2: JsonSerializationContext): JsonElement = {
//    new JsonPrimitive(obj.toString)
//  }
//}
//
//class OptionSerializer extends JsonSerializer[Option[_]] {
//  def serialize(obj: Option[_],  arg1: Type, arg2: JsonSerializationContext): JsonElement = {
//    new JsonPrimitive(obj.get.toString)
//  }
//}
//
//object JsonUtil {
//  def toJson(obj: Any): JsValue = {
////    val gsonBuilder = new GsonBuilder()
////    gsonBuilder.registerTypeAdapter(classOf[Enumeration#Value], new EnumSerializer())
////    gsonBuilder.registerTypeAdapter(classOf[Option[Double]], new OptionSerializer())
////    val gson = gsonBuilder.create()
//    obj.toJson.compactPrint
////    val json = gson.toJson(obj)
//    val json = obj.toJson.compactPrint
//    Json.parse(json)
//  }
//}
