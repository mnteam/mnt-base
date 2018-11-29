package com.mnt.base.evaluator;

import java.util.regex.Pattern;

/**
 * Created by BaiYang on 28/11/2018
 */
public class RegexEvaluator extends AbstractEvaluator {
    private Pattern pattern;

    public RegexEvaluator(String expression) {
        this.expression = expression;
        parse();
    }

    @Override
    public void parse() {
        this.pattern = Pattern.compile(this.expression);
    }

    @Override
    public Object eval(Object[] ms) {
        return pattern;
    }
}
