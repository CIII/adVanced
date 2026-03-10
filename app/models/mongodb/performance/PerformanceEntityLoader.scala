package models.mongodb.performance

import org.mongodb.scala.MongoCollection
import org.mongodb.scala.bson.{BsonArray, BsonBoolean, BsonDouble, BsonInt32, BsonInt64, BsonNull, BsonString, BsonValue, Document}
import models.mongodb.MongoExtensions._
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import play.api.Logger
import scala.concurrent.Await
import scala.concurrent.duration._

class PerformanceEntityLoader[T <: PerformanceEntity](
  fields: List[PerformanceField],
  collection: MongoCollection[Document],
  entityFactory: () => T
){
  val logger = Logger(this.getClass)
  private var pipeline: ListBuffer[Document] = ListBuffer()

  private def anyToBson(v: Any): BsonValue = v match {
    case s: String  => new BsonString(s)
    case i: Int     => new BsonInt32(i)
    case l: Long    => new BsonInt64(l)
    case d: Double  => new BsonDouble(d)
    case b: Boolean => new BsonBoolean(b)
    case null       => new BsonNull()
    case other      => new BsonString(other.toString)
  }

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

    var matchDoc = Document()
    for(fieldName <- filters.map { filter => filter.field.fieldName }.distinct){
      var fieldDoc = Document()
      for((op, values) <- filterMap(fieldName).toList){
        if(op.equalsIgnoreCase("in")){
          val bsonList = new BsonArray(values.map(anyToBson).asJava)
          fieldDoc = fieldDoc ++ Document("$in" -> bsonList)
        } else {
          fieldDoc = fieldDoc ++ Document(("$" + op) -> anyToBson(values.head))
        }
      }
      matchDoc = matchDoc ++ Document(fieldName -> fieldDoc)
    }

    pipeline += Document("$match" -> matchDoc)
    this
  }

  def withGroupSumStage(groupFields: List[PerformanceField] = fields): PerformanceEntityLoader[T] = {
    var dimensionDoc = Document()
    var groupDoc = Document()

    for(field <- groupFields.filter { field => field.fieldType == dimension }){
      dimensionDoc = dimensionDoc ++ Document(field.fieldName -> formatFieldName(field.fieldName))
    }

    groupDoc = groupDoc ++ Document("_id" -> dimensionDoc)
    for(field <- groupFields.filter{
      field => field.fieldType == measure && !field.isInstanceOf[CalculatedPerformanceField]
    }){
      groupDoc = groupDoc ++ Document(field.fieldName -> Document("$sum" -> formatFieldName(field.fieldName)))
    }

    pipeline += Document("$group" -> groupDoc)
    this
  }

  def withSortStage(direction: Int): PerformanceEntityLoader[T] = {
    pipeline += Document("$sort" -> Document("_id" -> {
      if(direction > 0){
        1
      } else {
        -1
      }
    }))

    this
  }

  def withPaginationStages(page: Int, pageSize: Int): PerformanceEntityLoader[T] = {
    pipeline += Document("$skip" -> (page-1) * pageSize)
    pipeline += Document("$limit" -> pageSize)
    this
  }

  def withCountStage: PerformanceEntityLoader[T] = {
    pipeline += Document("$count" -> "totalCount")
    this
  }

  def withNonQueryProject(projectFields: List[PerformanceField] = fields): PerformanceEntityLoader[T] = {
    var doc = Document()
    projectFields.foreach { field =>
      if(field.fieldType == dimension){
        doc = doc ++ Document(field.fieldName -> ("$_id." + field.fieldName))
      } else {
        doc = doc ++ Document(field.fieldName -> ("$" + field.fieldName))
      }
    }

    pipeline += Document("$project" -> doc)
    this
  }

  def withProject(projectFields: List[PerformanceField] = fields): PerformanceEntityLoader[T] = {
    var doc = Document()
    projectFields.foreach { field => doc = doc ++ field.projectionQueryObject }

    pipeline += Document("$project" -> doc)
    this
  }

  def withLookup(lookupCollection: MongoCollection[Document], localField: PerformanceField, foreignField: PerformanceField, as: String): PerformanceEntityLoader[T] = {
    pipeline += Document("$lookup" ->
      Document(
        "from" -> lookupCollection.namespace.getCollectionName,
        "localField" -> ("_id." + localField.fieldName),
        "foreignField" -> foreignField.fieldName,
        "as" -> as
      )
    )

    this
  }

  def withUnwind(path: String, preserveNullAndEmptyArrays: Boolean): PerformanceEntityLoader[T] = {
    pipeline += Document("$unwind" ->
      Document(
        "path" -> path,
        "preserveNullAndEmptyArrays" -> preserveNullAndEmptyArrays
      )
    )

    this
  }

  def execute: List[T] = {
    logger.info(pipeline.toList.toString)
    val results = Await.result(
      collection.aggregate(pipeline.toList).toFuture(),
      30.seconds
    )
    results.toList.map {
      doc =>
        val t = entityFactory()
        t.fromDocument(doc)
        t
    }
  }

  def executeCount: Int = {
    val results = Await.result(
      collection.aggregate(pipeline.toList).toFuture(),
      30.seconds
    )
    if(results.size > 0){
      results.head match {
        case head => head.getInteger("totalCount")
        case _=> 0
      }
    } else {
      0
    }
  }
}
