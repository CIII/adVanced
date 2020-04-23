package models.mongodb.performance

import models.mongodb.performance.CalculatedPerformanceField._
import models.mongodb.performance.PerformanceField.PerformanceFieldType._
import com.mongodb.casbah.Imports._
import com.mongodb.util.JSON

class CalculatedPerformanceField(
  fieldName: String,
  expression: Expression
) extends PerformanceField(fieldName, measure){
  
  override def dependantFields(): List[PerformanceField] = expression.getRequiredFields()
  override def projectionQueryObject(): DBObject = { 
    DBObject(fieldName -> JSON.parse("{" + expression.getValue + "}").asInstanceOf[DBObject]) 
  }
}

object CalculatedPerformanceField{
  
  trait Expression {
    def getValue(): Any
    def getRequiredFields(): List[PerformanceField]
  }
  
  abstract class Operation(expressions: Expression*) extends Expression{
    override def getValue(): Any = build()
    override def getRequiredFields(): List[PerformanceField] = expressions.map { 
      expression => expression.getRequiredFields()
    }.flatten.distinct.toList
    
    def getOperator(): String
    def build(): String = {
      getOperator() + ": [" +
        expressions.map { 
          expression => 
            if(expression.isInstanceOf[Operation]){
              "{" + expression.getValue + "}"
            } else {
              expression.getValue
            }
          }.mkString(", ") +
      "]"
    }
  }
  
  class Add(expressions: Expression*) extends Operation(expressions:_*){
    override def getOperator(): String = {
      "$add"
    }
  }
  
  class Subtract(expressions: Expression*) extends Operation(expressions:_*){
    override def getOperator(): String = {
      "$subtract"
    }
  }
  
  class Multiply(expressions: Expression*) extends Operation(expressions:_*){
    override def getOperator(): String = {
      "$multiply"
    }
  }
  
  class Divide(expressions: Expression*) extends Operation(expressions:_*){
    if(expressions.size != 2){
      throw new Exception("Divide must take exactly 2 arguments")
    }
    
    override def getOperator(): String = {
      "$divide"
    }
    
    /**
     * Divide needs to take div by 0 into account, so we need to wrap
     * the build call in a conditional that checks if the divisor is 0
     */
    override def build(): String = {
      var eqStage: Any = if(expressions(1).isInstanceOf[Operation]){
        "{" + expressions(1).getValue + "}" 
      } else {
        expressions(1).getValue  
      }
      
      "$cond: [" +
        "{ $eq: [" + eqStage + ", 0] }, 0, " +
        "{" + super.build() + "}" +
      "]"
    }
  }
  
  class Literal(value: Any) extends Expression {
    override def getValue(): Any = value
    override def getRequiredFields(): List[PerformanceField] = List()
  }
  
  class Variable(field: PerformanceField) extends Expression{
    override def getValue(): Any = "\"$" + field.fieldName + "\""
    override def getRequiredFields(): List[PerformanceField] = List(field)
  }
}