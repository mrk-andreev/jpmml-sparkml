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

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.ml.feature.StandardScalerModel;
import org.apache.spark.mllib.linalg.Vector;
import org.dmg.pmml.DataType;
import org.dmg.pmml.DerivedField;
import org.dmg.pmml.Expression;
import org.dmg.pmml.FieldName;
import org.dmg.pmml.FieldRef;
import org.dmg.pmml.OpType;
import org.jpmml.converter.PMMLUtil;
import org.jpmml.converter.ValueUtil;

public class StandardScalerModelConverter extends FeatureConverter<StandardScalerModel> {

	public StandardScalerModelConverter(StandardScalerModel transformer){
		super(transformer);
	}

	@Override
	public List<Feature> encodeFeatures(FeatureMapper featureMapper){
		StandardScalerModel transformer = getTransformer();

		List<Feature> inputFeatures = featureMapper.getFeatures(transformer.getInputCol());

		Vector mean = transformer.mean();
		if(transformer.getWithMean() && mean.size() != inputFeatures.size()){
			throw new IllegalArgumentException();
		}

		Vector std = transformer.std();
		if(transformer.getWithStd() && std.size() != inputFeatures.size()){
			throw new IllegalArgumentException();
		}

		List<Feature> result = new ArrayList<>();

		for(int i = 0; i < inputFeatures.size(); i++){
			ContinuousFeature inputFeature = (ContinuousFeature)inputFeatures.get(i);

			Expression expression = new FieldRef(inputFeature.getName());

			if(transformer.getWithMean()){
				double meanValue = mean.apply(i);

				if(!ValueUtil.isZero(meanValue)){
					expression = PMMLUtil.createApply("-", expression, PMMLUtil.createConstant(meanValue));
				}
			} // End if

			if(transformer.getWithStd()){
				double stdValue = std.apply(i);

				if(!ValueUtil.isOne(stdValue)){
					expression = PMMLUtil.createApply("/", expression, PMMLUtil.createConstant(stdValue));
				}
			}

			FieldName name = FieldName.create(transformer.getOutputCol() + "[" + String.valueOf(i) + "]");

			DerivedField derivedField = new DerivedField(OpType.CONTINUOUS, DataType.DOUBLE)
				.setName(name)
				.setExpression(expression);

			featureMapper.putDerivedField(derivedField);

			ContinuousFeature feature = new ContinuousFeature(name);

			result.add(feature);
		}

		return result;
	}
}