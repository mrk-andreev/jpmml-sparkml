/*
 * Copyright (c) 2016 Villu Ruusmann
 *
 * This file is part of JPMML-SparkML
 *
 * JPMML-SparkML is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * JPMML-SparkML is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with JPMML-SparkML.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpmml.sparkml;

import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Equivalence;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.general_regression.GeneralRegressionModel;
import org.jpmml.evaluator.ResultField;
import org.jpmml.evaluator.testing.ArchiveBatch;
import org.jpmml.evaluator.testing.PMMLEquivalence;
import org.jpmml.sparkml.model.HasRegressionTableOptions;
import org.jpmml.sparkml.model.HasTreeOptions;
import org.junit.Test;

public class RegressionTest extends SparkMLTest implements Algorithms, Datasets {

	@Override
	public ArchiveBatch createBatch(String name, String dataset, Predicate<ResultField> predicate, Equivalence<Object> equivalence){
		predicate = excludePredictionFields(predicate);

		ArchiveBatch result = new SparkMLTestBatch(name, dataset, predicate, equivalence){

			@Override
			public RegressionTest getIntegrationTest(){
				return RegressionTest.this;
			}

			@Override
			public Map<String, Object> getOptions(String name, String dataset){
				Map<String, Object> options = super.getOptions(name, dataset);

				if((LINEAR_REGRESION).equals(name) && (AUTO).equals(dataset)){
					options.put(HasRegressionTableOptions.OPTION_REPRESENTATION, GeneralRegressionModel.class.getSimpleName());
				} // End if

				if((DECISION_TREE).equals(name) || (GBT).equals(name) || (RANDOM_FOREST).equals(name)){
					options.put(HasTreeOptions.OPTION_ESTIMATE_FEATURE_IMPORTANCES, Boolean.TRUE);
				}

				return options;
			}
		};

		return result;
	}

	@Test
	public void evaluateDecisionTreeAuto() throws Exception {
		evaluate(DECISION_TREE, AUTO);
	}

	@Test
	public void evaluateGBTAuto() throws Exception {
		evaluate(GBT, AUTO);
	}

	@Test
	public void evaluateGLMAuto() throws Exception {
		evaluate(GLM, AUTO);
	}

	@Test
	public void evaluateLinearRegressionAuto() throws Exception {
		FieldName[] transformFields = {FieldName.create("mpgBucket")};

		evaluate(LINEAR_REGRESION, AUTO, excludeFields(transformFields));
	}

	@Test
	public void evaluateModelChainAuto() throws Exception {
		evaluate(MODEL_CHAIN, AUTO);
	}

	@Test
	public void evaluateRandomForestAuto() throws Exception {
		evaluate(RANDOM_FOREST, AUTO);
	}

	@Test
	public void evaluateDecisionTreeHousing() throws Exception {
		evaluate(DECISION_TREE, HOUSING);
	}

	@Test
	public void evaluateGLMHousing() throws Exception {
		evaluate(GLM, HOUSING, new PMMLEquivalence(1e-12, 1e-12));
	}

	@Test
	public void evaluateLinearRegressionHousing() throws Exception {
		evaluate(LINEAR_REGRESION, HOUSING);
	}

	@Test
	public void evaluateRandomForestHousing() throws Exception {
		evaluate(RANDOM_FOREST, HOUSING);
	}

	@Test
	public void evaluateGLMFormulaVisit() throws Exception {
		evaluate(GLM, VISIT, new PMMLEquivalence(1e-12, 1e-12));
	}
}