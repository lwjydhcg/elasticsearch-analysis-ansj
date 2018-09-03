package org.ansj.elasticsearch.index.analyzer;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ansj.domain.Result;
import org.ansj.domain.Term;
import org.ansj.recognition.arrimpl.NumRecognition;
import org.ansj.recognition.arrimpl.PersonRecognition;
import org.ansj.recognition.arrimpl.UserDefineRecognition;
import org.ansj.splitWord.Analysis;
import org.ansj.util.AnsjReader;
import org.ansj.util.Graph;
import org.ansj.util.TermUtil.InsertTermType;
import org.nlpcn.commons.lang.tire.GetWord;
import org.nlpcn.commons.lang.tire.domain.Forest;
import org.nlpcn.commons.lang.util.ObjConver;

/**
 * 标准分词
 * 
 * @author ansj
 * 
 */
public class OptAnalysis extends Analysis {

	/**
	 * nz 其他专有名词（除人名、地名、国名、团体）,例如满族、耐克、甲午战争。
	 * ns 地名
	 * nr 人名
	 * nt 机构团体,例如联合国、外交部。
	 */
	protected static final Set<String> RETAIN__NATURES = new HashSet<>(Arrays.asList(new String[] { "nr", "ns", "nt", "nz" }));

	@Override
	protected List<Term> getResult(final Graph graph) {

		Merger merger = new Merger() {
			@Override
			public List<Term> merger() {

				graph.walkPath();

				// 姓名识别
				if (graph.hasPerson && isNameRecognition) {
					// 人名识别
					new PersonRecognition().recognition(graph);
				}

				// 数字发现
				if (isNumRecognition) {
					new NumRecognition(isQuantifierRecognition && graph.hasNumQua).recognition(graph);
				}

				// 用户自定义词典的识别
				userDefineRecognition(graph, forests);

				return getResult();
			}

			private void userDefineRecognition(final Graph graph, Forest... forests) {
				new UserDefineRecognition(InsertTermType.SKIP, forests).recognition(graph);
				graph.rmLittlePath();
				graph.walkPathByScore();
			}

			private List<Term> getResult() {
				String temp = null;
				String nature = null;
				Set<String> set = new HashSet<String>();
				List<Term> result = new ArrayList<Term>();
				List<TermFL> resultFL = new ArrayList<TermFL>();
				int length = graph.terms.length - 1;
				for (int i = 0; i < length; i++) {
					if (graph.terms[i] != null) {
						resultFL.add(new TermFL(graph.terms[i]));
						result.add(graph.terms[i]);
						set.add(graph.terms[i].getName() + graph.terms[i].getOffe());
					}
				}

				// 排序
				Collections.sort(resultFL);

				LinkedList<Term> last = new LinkedList<Term>();
				char[] chars = graph.chars;
				if (forests != null) {
					for (Forest forest : forests) {
						if (forest == null) {
							continue;
						}
						GetWord word = forest.getWord(chars);
						while ((temp = word.getAllWords()) != null) {
							nature = word.getParam(0);
							if (temp.length() > 1 
									&& !set.contains(temp + word.offe) 
									&& RETAIN__NATURES.contains(nature)
									&& filters(resultFL, temp.length(), word.offe)) {
								set.add(temp + word.offe);
								last.add(new Term(temp, word.offe, nature, ObjConver.getIntValue(word.getParam(1))));
							}
						}
					}
				}
				result.addAll(last);

				// 不再排序，防止双引号短语查询slop bug 
				Collections.sort(result, new Comparator<Term>() {
				
					@Override
					public int compare(Term o1, Term o2) {
						if (o1.getOffe() == o2.getOffe()) {
							return o2.getName().length() - o1.getName().length();
						} else {
							return o1.getOffe() - o2.getOffe();
						}
					}
				});
				setRealName(graph, result);
				return result;
			}
		};
		return merger.merger();
	}

	public boolean filters(List<TermFL> termfls, int size, int offe) {
		for (TermFL item : termfls) {
			// 过滤非正常分词term开头的分词，term只有一个字时，from往前推一位
			// 在/p济南/ns召开/v（南召）
			// 旅游/n和/p服务/n（和服）
			// 改革/v示范县/n（范县）
			int from = item.from;
			if (item.word.length() == 1) {
				from -= 1;
			}
			if (from < offe && offe < item.dest) {
				return false;
			}
		}
		return true;
	}

	public OptAnalysis() {
		super();
	}

	public OptAnalysis(Reader reader) {
		super.resetContent(new AnsjReader(reader));
	}

	public static Result parse(String str) {
		return new OptAnalysis().parseStr(str);
	}

	public static Result parse(String str, Forest... forests) {
		return new OptAnalysis().setForests(forests).parseStr(str);
	}
}
