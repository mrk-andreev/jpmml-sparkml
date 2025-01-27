import java.io.File

import ml.dmlc.xgboost4j.scala.spark.{TrackerConf, XGBoostClassifier}
import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.feature._
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.sql.functions.{lit, udf}
import org.apache.spark.sql.types.{DataType, StringType, StructType}
import org.jpmml.sparkml.{DatasetUtil, PipelineModelUtil}

var df = DatasetUtil.loadCsv(spark, new File("csv/Iris.csv"))

val schema = df.schema
val floatSchema = DataType.fromJson(schema.json.replaceAll("double", "float"))

df = DatasetUtil.castColumns(df, floatSchema.asInstanceOf[StructType])

DatasetUtil.storeSchema(df, new File("schema/Iris.json"))

val labelIndexer = new StringIndexer().setInputCol("Species").setOutputCol("idx_Species")
val labelIndexerModel = labelIndexer.fit(df)

val assembler = new VectorAssembler().setInputCols(Array("Sepal_Length", "Sepal_Width", "Petal_Length", "Petal_Width")).setOutputCol("featureVector")

val trackerConf = TrackerConf(0, "scala")
val classifier = new XGBoostClassifier(Map("objective" -> "multi:softprob", "num_class" -> 3, "num_round" -> 17, "tracker_conf" -> trackerConf)).setLabelCol(labelIndexer.getOutputCol).setFeaturesCol(assembler.getOutputCol)

val pipeline = new Pipeline().setStages(Array(labelIndexer, assembler, classifier))
val pipelineModel = pipeline.fit(df)

PipelineModelUtil.storeZip(pipelineModel, new File("pipeline/XGBoostIris.zip"))

val predLabel = udf{ (value: Float) => labelIndexerModel.labels(value.toInt) }
val vectorToColumn = udf{ (vec: Vector, index: Int) => vec(index).toFloat }

var xgbDf = pipelineModel.transform(df)
xgbDf = xgbDf.selectExpr("prediction", "probability")
xgbDf = xgbDf.withColumn("Species", predLabel(xgbDf("prediction"))).drop("prediction")
xgbDf = xgbDf.withColumn("probability(setosa)", vectorToColumn(xgbDf("probability"), lit(0))).withColumn("probability(versicolor)", vectorToColumn(xgbDf("probability"), lit(1))).withColumn("probability(virginica)", vectorToColumn(xgbDf("probability"), lit(2))).drop("probability").drop("probability")

DatasetUtil.storeCsv(xgbDf, new File("csv/XGBoostIris.csv"))
