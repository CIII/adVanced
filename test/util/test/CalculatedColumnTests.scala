package util.test

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import org.junit.Before
import org.junit.Assert._
import org.hamcrest.CoreMatchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import util.charts.columns.CalculatedColumn._
import org.junit.Ignore

class CalculatedColumnTests extends AssertionsForJUnit {

  var expression1: Expression = _
  var expression2: Expression = _
  var expression3: Expression = _
  var expression4: Expression = _
  var expression5: Expression = _
  var expression6: Expression = _
  var expression7: Expression = _
  var expression8: Expression = _
  var expression9: Expression = _
  var expression10: Expression = _
  
  @Before
  def init {
    // basic
    expression1 = new Add(new Literal(2), new Literal(2))
    expression2 = new Subtract(new Literal(2), new Literal(2))
    expression3 = new Multiply(new Literal(2), new Literal(2))
    expression4 = new Divide(new Literal(2), new Literal(2))
    expression5 = new Divide(new Literal(2), new Literal(0))
    expression6 = new Subtract(new Literal(4), new Add(new Literal(2), new Literal(0))) 
    expression7 = new Divide(new Add(new Literal(2), new Literal(2)), new Literal(4))   // (2 + 2) / 4 
    expression8 = new Add(new Literal(2), new Divide(new Literal(2), new Literal(4)))   // 2 + (2 / 4) 
    expression9 = new Add(new Variable("cost"), new Variable("clicks")) 
    expression10 = new Add(new Variable("cost"), new Variable("clicks"), new Variable("clicks"))
  }
  
  @Test
  def testBasicExpressions(){
    assertThat("Bad evaluation for expression1", expression1.getValue.toString, is(equalTo("$add: [2, 2]")))
    assertThat("Bad evaluation for expression2", expression2.getValue.toString, is(equalTo("$subtract: [2, 2]")))
    assertThat("Bad evaluation for expression3", expression3.getValue.toString, is(equalTo("$multiply: [2, 2]")))
    assertThat("Bad evaluation for expression4", expression4.getValue.toString, is(equalTo("$cond: [{ $eq: [2, 0] }, 0, {$divide: [2, 2]}]")))
  }
  
  @Test
  def testDivideByZero(){
    assertThat("Bad evaluation for expression5", expression5.getValue.toString, is(equalTo("$cond: [{ $eq: [0, 0] }, 0, {$divide: [2, 0]}]")))
  }
  
  @Test
  def testNestedExpressions(){
    assertThat("Bad evaluation for expression6", expression6.getValue.toString, is(equalTo("$subtract: [4, {$add: [2, 0]}]")))
    assertThat("Bad evaluation for expression7", expression7.getValue.toString, is(equalTo("$cond: [{ $eq: [4, 0] }, 0, {$divide: [{$add: [2, 2]}, 4]}]")))
    assertThat("Bad evaluation for expression8", expression8.getValue.toString, is(equalTo("$add: [2, {$cond: [{ $eq: [4, 0] }, 0, {$divide: [2, 4]}]}]")))
  }
  
  @Test
  def testVariables(){
    assertThat("Bad evaluation for expression9", expression9.getValue.toString, is(equalTo("$add: [\"$cost\", \"$clicks\"]")))
  }
  
  @Test
  def testRequiredFields(){
    assertThat("Incorrect required fields for expression9", expression9.getRequiredFields.toString, is(equalTo("List(cost, clicks)")))
    assertThat("Incorrect required fields for expression10", expression10.getRequiredFields.toString, is(equalTo("List(cost, clicks)")))
  }
}