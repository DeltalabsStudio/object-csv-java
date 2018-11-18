package com.sahurjt.objectcsv;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Dictionary;
import java.util.Date;
import com.sahurjt.objectcsv.annotations.CsvColumn;
import com.sahurjt.objectcsv.annotations.CsvModel;
import com.sahurjt.objectcsv.annotations.CsvParameter;

/*
 * This class will help in assigning values to given property of generic model from given dictionary 
 * having common propertyName or annotation indicating property name. 
 * */

/*
 * Make this class workable even when we don't have csv headers
 * ie without dictionary 
 * */

final class GenericModelAdapter<T> {

	/// variable holding instance of class generic class T.
	private T classInstance;

	/// Dictionary that holds key/value pair of data
	private Dictionary<String, String> dictionary;

	private static final String DATE_FORMAT = "yyyy-MM-dd";

	public GenericModelAdapter(Class<T> classGeneric, Dictionary<String, String> dictionary)
			throws ObjectCsvException {
		this(classGeneric);
		this.dictionary = dictionary;
	}

	public GenericModelAdapter(Class<T> classGeneric) throws ObjectCsvException {
		try {
			classInstance = classGeneric.newInstance();
		} catch (IllegalAccessException | InstantiationException e) {
			throw new ObjectCsvException("Exception in creating instance : " + e.getMessage());
		}
	}

	public void setDictionary(Dictionary<String, String> dictionary) {
		this.dictionary = dictionary;
	}

	public T getModelInstance() {
		return classInstance;
	}

	protected T MapDictionaryToObject() throws ObjectCsvException {
		if (this.dictionary == null)
			throw new ObjectCsvException(
					"Dictionary is null.It must be assigned before calling map function.");
		MapDictionaryToObject(this.dictionary);
		return classInstance;
	}

	protected T MapDictionaryToObject(Dictionary<String, String> dictionary) {
		this.dictionary = dictionary;
		// complete logic to map
		try {
			PopulateModelFromDictionary();
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return classInstance;
	}

	/// assign dict values to model
	private void PopulateModelFromDictionary() throws IllegalArgumentException, IllegalAccessException {

		CsvModel csvAnnotation = classInstance.getClass().getAnnotation(CsvModel.class);
		if (csvAnnotation == null)
			return;
		boolean useColumnIndexing = !csvAnnotation.headerPresent();

		for (Field field : classInstance.getClass().getFields()) {

			String fieldId = getFieldIdToSearchInDictionary(field, useColumnIndexing);
			if (fieldId == null)
				return;

			String fieldValue = dictionary.get(fieldId);
			if (fieldValue == null)
				continue;

			AssignValue(field, fieldValue);
		}

	}

	private boolean AssignValue(Field field, String fieldValue) {

		try {
			if (field.getType() == int.class) {
				field.set(classInstance, Integer.valueOf(fieldValue));
			} else if (field.getType() == boolean.class) {
				field.set(classInstance, Boolean.valueOf(fieldValue));
			} else if (field.getType() == double.class) {
				field.set(classInstance, Double.valueOf(fieldValue));
			} else if (field.getType() == Date.class) {
				DateFormat df = new SimpleDateFormat(DATE_FORMAT);
				field.set(classInstance, df.parse(fieldValue));
			} else {
				field.set(classInstance, fieldValue);
			}
			return true;
		} catch (Exception e) {
			System.err.println(
					"Unable to parse " + fieldValue + " to type " + field.getType() + " | Reason: " + e.getMessage());
			return false;
		}
	}

	private String getFieldIdToSearchInDictionary(final Field field, boolean useColumnIndexing) {

		String keyToSearch;
		if (useColumnIndexing) {
			CsvColumn csvParamAnnotation = field.getAnnotation(CsvColumn.class);
			if (csvParamAnnotation == null)
				return null;
			keyToSearch = String.valueOf(csvParamAnnotation.coloumnIndex());
		} else {
			CsvParameter csvParamAnnotation = field.getAnnotation(CsvParameter.class);
			if (csvParamAnnotation == null)
				return null;
			keyToSearch = String.valueOf(csvParamAnnotation.value());
		}
		return keyToSearch.trim();
	}
}
