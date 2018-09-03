package org.ansj.elasticsearch.index.test;

import org.ansj.elasticsearch.index.analyzer.OptAnalysis;

public class Test {
	public static void main(String[] args) {
		System.out.println(new OptAnalysis().parseStr("东辽县人民政府"));
	}
}
