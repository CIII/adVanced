package util.test

import org.scalatest.junit.AssertionsForJUnit
import org.junit.Test
import org.junit.Before
import org.junit.Assert._
import org.hamcrest.CoreMatchers._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import util.charts.columns.HtmlColumn._
import org.junit.Ignore
import util.charts.columns.EditColumn

class HtmlColumnTests extends AssertionsForJUnit{
  
  var htmlBuilder1: HtmlColumnBuilder = _
  var htmlBuilder2: HtmlColumnBuilder = _
  var htmlBuilder3: HtmlColumnBuilder = _
  
  @Before
  def init(){
    htmlBuilder1 = new HtmlColumnBuilder(
      new HtmlSegment("<div>"),
      new HtmlSegment("</div>")
    )
    
    htmlBuilder2 = new HtmlColumnBuilder(   
      new HtmlSegment("<div>"),
        new StringColumnSegment("day"),
        new HtmlSegment("""<span class="pull-right">"""),
          new HtmlSegment("""<a href="/google/mcc/campaign/"""),
          new StringColumnSegment("day"),
          new HtmlSegment("""/"><i class="fa fa-edit"></i></a>"""),
        new HtmlSegment("</span>"),
      new HtmlSegment("</div>")
    )
    
    htmlBuilder3 = new HtmlColumnBuilder(
      new HtmlSegment("<div>"),
        new StringColumnSegment("date"),
        new NumberColumnSegment("cost"),
      new HtmlSegment("</div>")
    )
  }
  
  @Test
  def testBasicHtmlSegments(){
    assertThat("Invalid string result for html builder 1", 
      htmlBuilder1.build(),
      is(equalTo("$concat: [\"<div>\",\"</div>\"]"))
    )
        
    assertThat("Invalid string result for html builder 2",
      htmlBuilder2.build(),
      is(equalTo("$concat: [\"<div>\",\"$_id.day\",\"<span class=\"pull-right\">\",\"<a href=\"/google/mcc/campaign/\",\"$_id.day\",\"/\"><i class=\"fa fa-edit\"></i></a>\",\"</span>\",\"</div>\"]"))
    )
  }
  
  @Test
  def testNumberSegments(){
    assertThat("Invalid string result for html builder 1", 
      htmlBuilder3.build(),
      is(equalTo("$concat: [\"<div>\",\"$_id.date\",\"{ $substr: [$cost,0,-1]}\",\"</div>\"]"))
    )
  }
  
  @Test
  def testEditColumn(){
    assertThat("Invalid string result for edit column",
      new EditColumn("adEdit", "ad edit", "Ad", "adId", "/google/mcc/campaign/adgroup/ad/").getQueryObject().toString,
      is(equalTo("{ \"$concat\" : [ \"<div>\" , \"$_id.adEdit\" , \"<span class=\\\"pull-right\\\">\" , \"<a href=\\\"/google/mcc/campaign/adgroup/ad/\" , \"$_id.adId\" , \"/\\\"><i class=\\\"fa fa-edit\\\"></i></a>\" , \"</span>\" , \"</div>\"]}"))
    )     
  }
  
  @Test
  def testGetRequiredFields(){
    assertThat("Invalid required fields for edit column",
      new EditColumn("adEdit", "ad edit", "Ad", "adId", "/google/mcc/campaign/adgroup/ad/").getRequiredFields().toString,
      is(equalTo("List(adEdit, adId)"))
    )
  }
}