package models.mongodb.performance

import com.mongodb.casbah.MongoCollection
import com.mongodb.casbah.Imports._
import com.mongodb.casbah.query._
import scala.collection.mutable.ListBuffer
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import play.api.Logger

class PerformanceEntityLoader[T <: PerformanceEntity](
  fields: List[PerformanceField],
  collection: MongoCollection,
  entityFactory: () => T
){
  private var pipeline: ListBuffer[DBObject] = ListBuffer()
  
  def formatFieldName(fieldName: String): String = {
    if(pipeline.size > 0){
      "$".concat(fieldName)
    } else {
      fieldName
    }
  }
  
  def withMatchStage(filters: List[PerformanceEntityFilter]): PerformanceEntityLoader[T] = {
    var filterMap: Map[String, ListBuffer[(String, List[Any])]] = Map()
    for(filter <- filters){
      if(filterMap.contains(filter.field.fieldName)){
        filterMap(filter.field.fieldName) += (filter.operation -> filter.values)
      } else {
        filterMap += (filter.field.fieldName -> ListBuffer((filter.operation -> filter.values)))
      }
    }
      
    pipeline += MongoDBObject("$match" -> {
      var matchBuilder = MongoDBObject.newBuilder
      for(fieldName <- filters.map { filter => filter.field.fieldName }.distinct){
        var fieldBuilder = MongoDBObject.newBuilder
        for((op, values) <- filterMap(fieldName).toList){
          if(op.equalsIgnoreCase("in")){
            fieldBuilder += ("$in" -> values)
          } else {
            fieldBuilder += (("$" + op) -> values.head)
          }
        }
        
        matchBuilder += (fieldName -> fieldBuilder.result)
      }
      
      matchBuilder.result
    })
    
    this
  }
  
  def withGroupSumStage(groupFields: List[PerformanceField] = fields): PerformanceEntityLoader[T] = {
    var dimensionBuilder = MongoDBObject.newBuilder
    var groupBuilder = MongoDBObject.newBuilder
    
    for(field <- groupFields.filter { field => field.fieldType == dimension }){
      dimensionBuilder += (field.fieldName -> formatFieldName(field.fieldName)) 
    }
    
    groupBuilder += ("_id" -> dimensionBuilder.result)
    for(field <- groupFields.filter{ 
      field => field.fieldType == measure && !field.isInstanceOf[CalculatedPerformanceField]
    }){
      groupBuilder += field.fieldName -> MongoDBObject("$sum" -> {
        formatFieldName(field.fieldName)
      })
    }
    
    pipeline += MongoDBObject("$group" -> groupBuilder.result)
    this
  }
  
  def withSortStage(direction: Int): PerformanceEntityLoader[T] = {
    pipeline += MongoDBObject("$sort" -> MongoDBObject("_id" -> {
      if(direction > 0){
        1
      } else {
        -1
      }
    }))
    
    this
  }
  
  def withPaginationStages(page: Int, pageSize: Int): PerformanceEntityLoader[T] = {
    pipeline += MongoDBObject("$skip" -> (page-1) * pageSize).result
    pipeline += MongoDBObject("$limit" -> pageSize).result
    this
  }
  
  def withCountStage: PerformanceEntityLoader[T] = {
    pipeline += MongoDBObject("$count" -> "totalCount")
    this
  }
  
  def withNonQueryProject(projectFields: List[PerformanceField] = fields): PerformanceEntityLoader[T] = {
    pipeline += MongoDBObject("$project" -> {
      var dbo: DBObject = DBObject()
      projectFields.foreach { field => 
        if(field.fieldType == dimension){
          dbo = dbo ++ (field.fieldName -> ("$_id." + field.fieldName))
        } else {
          dbo = dbo ++ (field.fieldName -> ("$" + field.fieldName))
        }
      }
      
      dbo
    })
    
    this
  }
  
  def withProject(projectFields: List[PerformanceField] = fields): PerformanceEntityLoader[T] = {
    pipeline += MongoDBObject("$project" -> {
      var dbo: DBObject = DBObject()
      projectFields.foreach { field => dbo = dbo ++ field.projectionQueryObject }
      dbo
    })
    
    this
  }
  
  def withLookup(lookupCollection: MongoCollection, localField: PerformanceField, foreignField: PerformanceField, as: String): PerformanceEntityLoader[T] = {
    pipeline += MongoDBObject("$lookup" -> 
      MongoDBObject(
        "from" -> lookupCollection.name,
        "localField" -> ("_id." + localField.fieldName),
        "foreignField" -> foreignField.fieldName,
        "as" -> as
      )
    )
    
    this
  }
  
  def withUnwind(path: String, preserveNullAndEmptyArrays: Boolean): PerformanceEntityLoader[T] = {
    pipeline += MongoDBObject("$unwind" ->
      MongoDBObject(
        "path" -> path,
        "preserveNullAndEmptyArrays" -> preserveNullAndEmptyArrays
      )
    )
    
    this
  }
  
  def execute: List[T] = {
    Logger.info(pipeline.toList.toString)
    collection.aggregate(pipeline.toList).results.toList.map { 
      dbo => 
        val t = entityFactory()
        t.fromDBO(dbo)
        t
    }
  }
  
  def executeCount: Int = {
    val results = collection.aggregate(pipeline.toList).results
    if(results.size > 0){
      results.head match {
        case head => head.getAs[Int]("totalCount").get
        case _=> 0
      }
    } else {
      0
    }
  }
}