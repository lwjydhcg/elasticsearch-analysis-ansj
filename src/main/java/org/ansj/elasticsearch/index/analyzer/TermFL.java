package org.ansj.elasticsearch.index.analyzer;

import org.ansj.domain.Term;

public class TermFL implements Comparable<TermFL> {

	String word;

	int from;

	int dest;

	public TermFL(Term term) {
		super();
		this.word = term.getName();
		this.from = term.getOffe();
		this.dest = term.getOffe() + term.getName().length();
	}

	@Override
	public int compareTo(TermFL o) {
		if (this.from == o.from) {
			return this.dest - o.dest;
		}
		return this.from - o.from;
	}

}
