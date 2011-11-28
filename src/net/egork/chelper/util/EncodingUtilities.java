package net.egork.chelper.util;

import com.intellij.openapi.project.Project;
import net.egork.chelper.task.MethodSignature;
import net.egork.chelper.task.StreamConfiguration;
import net.egork.chelper.task.Task;
import net.egork.chelper.task.Test;
import net.egork.chelper.task.TestType;
import net.egork.chelper.task.TopCoderTask;
import net.egork.chelper.task.TopCoderTest;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EncodingUtilities {
	public static final String TOKEN_SEPARATOR = "::";
	public static final String TEST_SEPARATOR = ";;";

	public static String encode(String s) {
		return s.replace(":", "/:").replace(";", "/;").replace("_", "/_").replace("\n", "/__").replace("\r", "");
	}

	public static String decode(String s) {
		return s.replace("/__", "\n").replace("/_", "_").replace("/:", ":").replace("/;", ";");
	}

	public static String encodeTests(Test[] tests) {
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (Test test : tests) {
			if (first)
				first = false;
			else
				builder.append(TOKEN_SEPARATOR);
			builder.append(encode(test));
		}
		if (builder.length() == 0)
			return "empty";
		return builder.toString();
	}

	public static String encode(Test test) {
		return encode(test.input) + TEST_SEPARATOR + encode(test.output) + TEST_SEPARATOR + Boolean.toString(
			test.active);
	}

	public static Test decodeTest(int index, String test) {
		String[] tokens = test.split(TEST_SEPARATOR, -1);
		return new Test(decode(tokens[0]), decode(tokens[1]), index, tokens.length == 2 || Boolean.valueOf(tokens[2]));
	}

	public static TopCoderTest decodeTopCoderTest(int index, String s, int argumentCount) {
		String[] tokens = s.split(TEST_SEPARATOR, -1);
		String[] arguments = new String[argumentCount];
		for (int i = 0; i < arguments.length; i++)
			arguments[i] = decode(tokens[i]);
		return new TopCoderTest(arguments, decode(tokens[argumentCount]), index, tokens.length == argumentCount + 1
			|| Boolean.parseBoolean(tokens[argumentCount + 1]));
	}

	public static String encode(TopCoderTest test) {
		StringBuilder builder = new StringBuilder();
		for (String argument : test.arguments)
			builder.append(encode(argument)).append(TEST_SEPARATOR);
		builder.append(encode(test.result)).append(TEST_SEPARATOR).append(test.active);
		return builder.toString();
	}

	public static String encodeTests(TopCoderTest[] tests) {
		if (tests.length == 0)
			return "empty";
		StringBuilder builder = new StringBuilder();
		boolean first = true;
		for (TopCoderTest test : tests) {
			if (first)
				first = false;
			else
				builder.append(TOKEN_SEPARATOR);
			builder.append(encode(test));
		}
		return builder.toString();
	}

	public static String encodeTask(Task task) {
		StringBuilder builder = new StringBuilder();
		builder.append(task.name).append(TOKEN_SEPARATOR).append(task.location).append(TOKEN_SEPARATOR).
			append(task.testType).append(TOKEN_SEPARATOR).append(task.input.type).append(TOKEN_SEPARATOR).
			append(task.input.type == StreamConfiguration.StreamType.CUSTOM ? task.input.fileName : "").append(
			TOKEN_SEPARATOR).append(task.output.type).append(TOKEN_SEPARATOR).
			append(task.output.type == StreamConfiguration.StreamType.CUSTOM ? task.output.fileName : "").append(
			TOKEN_SEPARATOR).append(task.heapMemory).append(TOKEN_SEPARATOR).append(task.stackMemory).
			append(TOKEN_SEPARATOR).append(task.truncate).append(TOKEN_SEPARATOR);
		builder.append(encodeTests(task.tests));
		return builder.toString();
	}

	public static Task readTask(String text, Project project) {
		String[] tokens = text.split(TOKEN_SEPARATOR, -1);
		String name = tokens[0];
		String location = tokens[1];
		TestType testType;
		try {
			testType = TestType.valueOf(tokens[2]);
		} catch (IllegalArgumentException e) {
			testType = null;
		}
		StreamConfiguration.StreamType inputType;
		try {
			inputType = StreamConfiguration.StreamType.valueOf(tokens[3]);
		} catch (IllegalArgumentException e) {
			inputType = null;
		}
		String inputFileName = null;
		if (inputType == StreamConfiguration.StreamType.CUSTOM)
			inputFileName = tokens[4];
		StreamConfiguration.StreamType outputType;
		try {
			outputType = StreamConfiguration.StreamType.valueOf(tokens[5]);
		} catch (IllegalArgumentException e) {
			outputType = null;
		}
		String outputFileName = null;
		if (outputType == StreamConfiguration.StreamType.CUSTOM)
			outputFileName = tokens[6];
		String heapMemory = tokens[7];
		String stackMemory = tokens[8];
		int index = 9;
		boolean truncate = true;
		if (tokens[index].equals("true") || tokens[index].equals("false"))
			truncate =  Boolean.parseBoolean(tokens[index++]);
		if ("empty".equals(tokens[index])) {
			return new Task(name, location, testType, new StreamConfiguration(inputType, inputFileName),
				new StreamConfiguration(outputType, outputFileName), heapMemory, stackMemory, project, truncate);
		}
		Test[] tests = new Test[tokens.length - index];
		for (int i = 0; i < tests.length; i++)
			tests[i] = decodeTest(i, tokens[index + i]);
		return new Task(name, location, testType, new StreamConfiguration(inputType, inputFileName),
			new StreamConfiguration(outputType, outputFileName), heapMemory, stackMemory, project, truncate, tests);
	}

	public static String encodeTask(TopCoderTask task) {
		StringBuilder builder = new StringBuilder();
		builder.append(task.name).append(TOKEN_SEPARATOR).append(task.signature == null ? "" : task.signature).append(
			TOKEN_SEPARATOR);
		builder.append(encodeTests(task.tests));
		return builder.toString();
	}

	public static TopCoderTask decodeTopCoderTask(String taskConf, Project project) {
		String[] tokens = taskConf.split(TOKEN_SEPARATOR, -1);
		String name = tokens[0];
		MethodSignature signature = MethodSignature.parse(tokens[1]);
		int argumentCount = signature != null ? signature.arguments.length : 0;
		if ("empty".equals(tokens[2]))
			return new TopCoderTask(project, name, signature);
		TopCoderTest[] tests = new TopCoderTest[tokens.length - 2];
		for (int i = 0; i < tests.length; i++)
			tests[i] = decodeTopCoderTest(i, tokens[i + 2], argumentCount);
		return new TopCoderTask(project, name, signature, tests);
	}
}
