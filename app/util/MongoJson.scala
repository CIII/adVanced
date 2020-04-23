package util

object MongoJson {
  import play.api.libs.json._
  import org.bson.types.ObjectId
  import com.mongodb.BasicDBObject
  import com.mongodb.BasicDBList
  import com.mongodb.DBObject
  import java.util.Date
  import scala.collection.JavaConversions

  /** Serializes the given [[com.mongodb.DBObject]] into a [[play.api.libs.json.JsValue]]. */
  def toJson(dbObject: DBObject) :JsValue = Json.toJson(dbObject)(BSONObjectFormat)

  /** Deserializes the given [[play.api.libs.json.JsValue]] into a [[com.mongodb.DBObject]]. */
  def fromJson(v: JsValue) :DBObject = Json.fromJson[DBObject](v)(BSONObjectFormat).asOpt.orNull

  /** Formatter for [[com.mongodb.DBObject]], handling serialization/deserialisation for DBObjects. */
  implicit object BSONObjectFormat extends Format[DBObject] {
    def reads(json: JsValue) :JsResult[DBObject] = JsSuccess(parse(json.asInstanceOf[JsObject]))
    def writes(bson: DBObject) :JsValue = Json.parse(bson.toString)

    private def parse(map: JsObject) :BasicDBObject = new BasicDBObject(
      JavaConversions.mapAsJavaMap(Map() ++ map.fields.map { p =>
        (p._1, p._2 match {
          case v: JsObject =>
            specialMongoJson(v).fold (
              normal => parse(normal),
              special => special
            )
          case v: JsArray => parse(v)
          case v: JsValue => parse(v)
        })
      })
    )

    private def specialMongoJson(json: JsObject) :Either[JsObject, Object] = {
      if(json.fields.nonEmpty) {
        json.fields.head match {
          case (k, v :JsString) if k == "$date" => Right(new Date(v.value.toLong))
          case (k, v :JsNumber) if k == "$date" => Right(new Date(v.value.toLong))
          case (k, v :JsString) if k == "$oid" => Right(new ObjectId( v.value ))
          case (k, v) if k.startsWith("$") => throw new RuntimeException("unsupported specialMongoJson " + k + " with v: " + v.getClass + ":" + v.toString())
          case _ => Left(json)
        }
      } else Left(json)

    }

    private def parse(array: JsArray) :BasicDBList = {
      val r = new BasicDBList()
      r.addAll(scala.collection.JavaConversions.seqAsJavaList(array.value map { v =>
        parse(v).asInstanceOf[Object]
      }))
      r
    }

    private def parse(v: JsValue) :Any = v match {
      case v: JsObject => parse(v)
      case v: JsArray => parse(v)
      case v: JsString => v.value
      case v: JsNumber => {
        val vd = v.as[Double]
        if(vd == Math.floor(vd)){
          v.as[Long]
        } else {
          vd
        }
      }
      case v: JsBoolean => v.value
      case JsNull => null
    }
  }

  implicit object ObjectIdFormat extends Format[ObjectId] {
    def reads(json: JsValue) :JsResult[ObjectId] = JsSuccess({
      json match {
        case obj: JsObject if obj.keys.contains("$oid") => new ObjectId( (obj \ "$oid").toString )
        case s: JsString => new ObjectId(s.value)
        case _ => throw new RuntimeException("unsupported ObjectId " + json)
      }
    })
    def writes(objectId: ObjectId) :JsObject = {
      JsObject(Seq("$oid" -> JsString(objectId.toString)))
    }
  }


  implicit object MongoDateFormat extends Format[Date] {
    def reads(json: JsValue) :JsResult[Date] = JsSuccess(json match {
      case obj: JsObject if obj.keys.contains("$date") => new Date((obj \ "$date").toString.toLong)
      case _ => throw new RuntimeException("unsupported Date " + json)
    })
    def writes(date: Date) :JsObject = JsObject( Seq("$date" -> JsString(date.getTime + "")) )
  }
}