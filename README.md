# KotlinLang 
[![Build Status](https://travis-ci.org/Tatskaari/KotlinLang.svg?branch=master)](https://travis-ci.org/Tatskaari/KotlinLang)

A toy language written in kotlin. The plan is to get this targeting the JVM however I'm just working on getting the 
parser working right now. The real goal of this project is simply to learn kotlin.

Another goal of this project is to try out a few idioms. You may have noticed the build passing tag. Every commit to 
this project is run through Travis CI and passed through a suite of unit tests. 

# Road Map
- Parsing
  - Code blocks - Done
  - Variables and assignment - Done
  - RPN expressions - Done
  - if statements
  - loops
  - input and output
  - function calls
  - variable types (decimal, character?)
- Interpreter
- byte code

# Syntax/Grammar 
Right now simple blocks only:

~~~~
{
    var a := + 12 12
}
~~~~

I will probably vaguely follow kotlins syntax and see where I end up. 