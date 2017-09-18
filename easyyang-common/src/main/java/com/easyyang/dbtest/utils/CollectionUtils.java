package com.easyyang.dbtest.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class CollectionUtils
{
  public static Collection select(Collection inputCollection, Predicate predicate)
  {
    List answer = new ArrayList(inputCollection.size());
    select(inputCollection, predicate, answer);
    return answer;
  }
  
  public static void select(Collection inputCollection, Predicate predicate, Collection outputCollection)
  {
    if ((inputCollection != null) && (predicate != null)) {
      for (Iterator iter = inputCollection.iterator(); iter.hasNext();)
      {
        Object item = iter.next();
        if (predicate.evaluate(item)) {
          outputCollection.add(item);
        }
      }
    }
  }
  
  public static boolean isEmpty(Collection coll)
  {
    return (coll == null) || (coll.isEmpty());
  }
  
  public static boolean isNotEmpty(Collection coll)
  {
    return !isEmpty(coll);
  }
}