package util

import com.google.gson.{ExclusionStrategy, FieldAttributes}

class PrimaryExclusionStrategy extends ExclusionStrategy {

  def shouldSkipClass(arg0: Class[_] ): Boolean = {
    false
  }

  def shouldSkipField(f: FieldAttributes): Boolean = {
    f.getName.equals("__equalsCalc") || f.getName.equals("__hashCodeCalc")
  }

}