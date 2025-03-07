package com.iisquare.fs.app.spark.util;

import scala.collection.JavaConverters;
import scala.collection.Seq;

import java.util.Collection;
import java.util.List;

public class ScalaUtil {

    public static <T> Seq<T> collection2seq(Collection<T> collection) {
        return JavaConverters.asScalaIteratorConverter(collection.iterator()).asScala().toSeq();
    }

    public static <T> List<T> seq2collection(Seq<T> seq) {
        return (List<T>) scala.collection.JavaConversions.seqAsJavaList(seq);
    }

}
