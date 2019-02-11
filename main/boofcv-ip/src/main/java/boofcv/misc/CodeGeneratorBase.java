/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.misc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;


/**
 * <p>Base class for code generators.</p>
 *
 * @author Peter Abeles
 */
public abstract class CodeGeneratorBase {

	protected PrintStream out;
	protected String className;
	protected boolean concurrent = false;

	public CodeGeneratorBase(boolean useDefaultName) {
		if( useDefaultName ) {
			autoSelectName();
		}
	}

	public CodeGeneratorBase() {
		autoSelectName();
	}

	public void autoSelectName() {
		String concurrentName = concurrent? "_MT" : "";

		className = getClass().getSimpleName()+concurrentName;
		if( className.startsWith("Generate") ) {
			int l = "Generate".length();
			className = className.substring(l);
			try {
				initFile();
			} catch( FileNotFoundException e ) {
				throw new RuntimeException(e);
			}
		} else {
			System.out.println("Class name doesn't start with Generate");
		}
	}

	protected void printParallel(String var, String lower, String upper , String body ) {
		out.println();
		if( concurrent ) {
//			body = body.replace("\n\t","\n\t\t");
			out.printf("\t\tBoofConcurrency.range(%s, %s, %s -> {\n",lower,upper,var);
			out.print(body);
			out.print("\t\t});\n");
		} else {
			out.printf("\t\tfor( int %s = %s; %s < %s; %s++ ) {\n",var,lower,var,upper,var);
			out.print(body);
			out.print("\t\t}\n");
		}
	}
	protected void printParallelBlock(String var0 , String var1, String lower, String upper , String minBlock , String body ) {
		out.println();
		if( concurrent ) {
			out.printf("\t\tBoofConcurrency.blocks(%s, %s, %s,(%s,%s)->{\n",lower,upper,minBlock,var0,var1);
			String[] lines = body.split("\n");
			for( String s : lines ) {
				if( !s.isEmpty() ) {
					s = "\t"+s;
				}
				out.print(s+"\n");
			}
			out.print("\t\t});\n");
		} else {
			out.printf("\t\tfinal int %s = %s, %s = %s;\n",var0,lower,var1,upper);
			out.print(body);
		}
	}

	/**
	 * Creates 
	 *
	 * @throws FileNotFoundException
	 */
	public abstract void generate() throws FileNotFoundException;

	public void initFile() throws FileNotFoundException {
		out = new PrintStream(new FileOutputStream(className + ".java"));
		out.print(CodeGeneratorUtil.copyright);
		out.println();
		out.println("package " + getPackage() + ";");
		out.println();
	}

	public void setOutputFile( String className ) throws FileNotFoundException {
		if( this.className != null )
			throw new IllegalArgumentException("ClassName already set.  Out of date code?");
		this.className = className;
		initFile();
	}

	public String generatedString() {
		return "@Generated(\""+getClass().getName()+"\")\n";
	}

	public String getPackage() {
		return getClass().getPackage().getName();
	}
}
