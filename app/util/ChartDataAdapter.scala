package util

import com.google.gson.JsonElement
import com.google.gson.JsonDeserializationContext
import scala.collection.JavaConverters._
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonSerializationContext

object ChartDataAdapter {
  
    def deserializeList(json: JsonElement, context: JsonDeserializationContext): List[Any] = {
      val sList = context.deserialize[java.util.List[java.lang.Object]](json, new TypeToken[java.util.ArrayList[java.lang.Object]]{}.getType).asScala
      sList.map { javaObj => javaObj.asInstanceOf[Any] }.toList
    }
    
    def deserializeMapOfLists(json: JsonElement, context: JsonDeserializationContext): Map[String, List[Any]] = {
      val jMap = context.deserialize[java.util.Map[String, java.util.List[java.lang.Object]]](json, new TypeToken[java.util.HashMap[String, java.util.ArrayList[java.lang.Object]]]{}.getType).asScala
      // convert to scala collections (automatically mutable unfortunately)
      val sMapMutable = jMap.map{ case (key, javaList) => (key, javaList.asScala.map { javaObj => javaObj.asInstanceOf[Any] })}
      // convert to immutable collections
      collection.immutable.Map(
          sMapMutable.map(kv => (kv._1, collection.immutable.List(kv._2.toList: _*))).toList: _*
      )
    }
    
    def deserializeListOfLists(json: JsonElement, context: JsonDeserializationContext): List[List[Any]] = {
      val sListjList = context.deserialize[java.util.List[java.util.List[java.lang.Object]]](
          json, 
          new TypeToken[java.util.ArrayList[java.util.ArrayList[java.lang.Object]]]{}.getType
      ).asScala
      
      sListjList.map { 
        jList => jList.asScala.map { 
          javaObj => javaObj.asInstanceOf[Any] 
        }.toList 
      }.toList
    }
    
    def serializeMap(sMap: Map[String, List[Any]], context: JsonSerializationContext): JsonElement = {
      context.serialize(sMap.map(kv => (kv._1, kv._2.asJava)).asJava)
    }
    
    def serializeListOfLists(sListsList: List[List[Any]], context: JsonSerializationContext): JsonElement = {
      context.serialize(sListsList.map { sList => sList.asJava }.asJava )  
    }
}