package net.kotiyasanae.chatserver.util;

import java.util.Random;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Calculator {
    private static final Random random = new Random();

    // 帮助文本
    private static final String HELP_TEXT =
            """
                    计算器使用说明:
                    基本运算: + - * / ( )
                    数学函数: sqrt(x), sin(x), cos(x), tan(x)
                             log(x), ln(x), abs(x)
                             pow(x,y), max(x,y), min(x,y)
                    常量: pi, e
                    示例: .calc sin(pi/2) + sqrt(16)""";

    public static String calculate(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return HELP_TEXT;
        }

        String expr = expression.trim();

        // 显示帮助
        if (expr.equalsIgnoreCase("help")) {
            return HELP_TEXT;
        }

        try {
            // 预处理表达式
            String processedExpr = preprocessExpression(expr);

            // 计算表达式
            double result = evaluateExpression(processedExpr);

            // 格式化结果
            return formatResult(expr, result);

        } catch (Exception e) {
            return "计算错误: " + e.getMessage();
        }
    }

    /**
     * 预处理表达式
     */
    private static String preprocessExpression(String expr) {
        // 转换为小写
        String processed = expr.toLowerCase();

        // 替换常量
        processed = processed.replace("pi", Double.toString(Math.PI));
        processed = processed.replace("e", Double.toString(Math.E));

        return processed;
    }

    /**
     * 计算表达式
     */
    private static double evaluateExpression(String expr) {
        // 处理函数调用
        expr = evaluateFunctions(expr);

        // 计算基本表达式
        return evaluateBasicExpression(expr);
    }

    /**
     * 处理数学函数
     */
    private static String evaluateFunctions(String expr) {
        // 处理单参数函数
        Pattern singleArgPattern = Pattern.compile("(sqrt|sin|cos|tan|log|ln|abs|ceil|floor|round|exp)\\(([^)]+)\\)");
        Matcher matcher = singleArgPattern.matcher(expr);

        while (matcher.find()) {
            String functionName = matcher.group(1);
            String argStr = matcher.group(2);
            double arg = evaluateBasicExpression(argStr);
            double result = applySingleArgFunction(functionName, arg);

            expr = expr.substring(0, matcher.start()) + result + expr.substring(matcher.end());
            matcher = singleArgPattern.matcher(expr);
        }

        // 处理双参数函数
        Pattern doubleArgPattern = Pattern.compile("(pow|max|min)\\(([^,]+),([^)]+)\\)");
        matcher = doubleArgPattern.matcher(expr);

        while (matcher.find()) {
            String functionName = matcher.group(1);
            String arg1Str = matcher.group(2);
            String arg2Str = matcher.group(3);
            double arg1 = evaluateBasicExpression(arg1Str);
            double arg2 = evaluateBasicExpression(arg2Str);
            double result = applyDoubleArgFunction(functionName, arg1, arg2);

            expr = expr.substring(0, matcher.start()) + result + expr.substring(matcher.end());
            matcher = doubleArgPattern.matcher(expr);
        }

        return expr;
    }

    /**
     * 应用单参数函数
     */
    private static double applySingleArgFunction(String functionName, double arg) {
        return switch (functionName) {
            case "sqrt" -> Math.sqrt(arg);
            case "sin" -> Math.sin(arg);
            case "cos" -> Math.cos(arg);
            case "tan" -> Math.tan(arg);
            case "log" -> Math.log10(arg);
            case "ln" -> Math.log(arg);
            case "abs" -> Math.abs(arg);
            case "ceil" -> Math.ceil(arg);
            case "floor" -> Math.floor(arg);
            case "round" -> Math.round(arg);
            case "exp" -> Math.exp(arg);
            default -> throw new IllegalArgumentException("未知函数: " + functionName);
        };
    }

    /**
     * 应用双参数函数
     */
    private static double applyDoubleArgFunction(String functionName, double arg1, double arg2) {
        return switch (functionName) {
            case "pow" -> Math.pow(arg1, arg2);
            case "max" -> Math.max(arg1, arg2);
            case "min" -> Math.min(arg1, arg2);
            default -> throw new IllegalArgumentException("未知函数: " + functionName);
        };
    }

    /**
     * 计算基本表达式（支持 + - * /）
     */
    private static double evaluateBasicExpression(String expr) {
        // 移除所有空格
        expr = expr.replaceAll("\\s+", "");

        // 使用栈来处理运算符优先级
        Stack<Double> numbers = new Stack<>();
        Stack<Character> operators = new Stack<>();

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);

            if (Character.isDigit(c) || c == '.') {
                // 处理数字
                StringBuilder sb = new StringBuilder();
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i));
                    i++;
                }
                i--;
                numbers.push(Double.parseDouble(sb.toString()));
            } else if (c == '(') {
                operators.push(c);
            } else if (c == ')') {
                while (operators.peek() != '(') {
                    numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.pop();
            } else if (isOperator(c)) {
                while (!operators.isEmpty() && hasPrecedence(c, operators.peek())) {
                    numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
                }
                operators.push(c);
            }
        }

        while (!operators.isEmpty()) {
            numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()));
        }

        return numbers.pop();
    }

    /**
     * 检查是否为运算符
     */
    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    /**
     * 检查运算符优先级
     */
    private static boolean hasPrecedence(char op1, char op2) {
        if (op2 == '(' || op2 == ')') {
            return false;
        }
        return (op1 != '*' && op1 != '/') || (op2 != '+' && op2 != '-');
    }

    /**
     * 应用运算符
     */
    private static double applyOperator(char operator, double b, double a) {
        return switch (operator) {
            case '+' -> a + b;
            case '-' -> a - b;
            case '*' -> a * b;
            case '/' -> {
                if (b == 0) throw new ArithmeticException("除以零");
                yield a / b;
            }
            default -> 0;
        };
    }

    /**
     * 格式化结果
     */
    private static String formatResult(String originalExpr, double value) {
        // 如果是整数，显示为整数形式
        if (value == (long) value) {
            return String.format("%s = %d", originalExpr, (long) value);
        } else {
            // 保留合理的小数位数
            String formatted = String.format("%.6f", value).replaceAll("0*$", "").replaceAll("\\.$", "");
            return String.format("%s = %s", originalExpr, formatted);
        }
    }

    /**
     * 生成随机数学题目
     */
    public static String generateRandomProblem() {
        String[] problems = {
                "1 + 2 * 3 - 4 / 2",
                "sqrt(16) + pow(2, 3)",
                "abs(-5) + round(3.14)",
                "max(5, 10) + min(3, 7)"
        };

        String problem = problems[random.nextInt(problems.length)];
        try {
            double result = evaluateExpression(preprocessExpression(problem));
            return "数学挑战: " + problem + " = ? (答案: " + formatResult("", result).substring(3) + ")";
        } catch (Exception e) {
            return "数学挑战: " + problem;
        }
    }
}