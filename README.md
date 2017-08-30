# KotlinLang 
[![Build Status](https://travis-ci.org/Tatskaari/KotlinLang.svg?branch=master)](https://travis-ci.org/Tatskaari/KotlinLang)
[![codecov](https://codecov.io/gh/Tatskaari/KotlinLang/branch/master/graph/badge.svg)](https://codecov.io/gh/Tatskaari/KotlinLang)

A toy language written in kotlin with the idea of being simple to pick up for beginners. The plan is to get this
targeting the JVM however I'm just working on getting the parser working right now. The real goal of this project is
simply to learn kotlin.

Another goal of this project is to try out a few idioms. You may have noticed the build passing tag. Every commit to 
this project is run through Travis CI and passed through a suite of unit tests. 

# Road Map
- Desired features
  - Code blocks - Done
  - Variables and assignment - Done
  - Expressions - Done
  - If statements - Done
  - While loops - Done
  - Input and output - Done
  - Function calls - Done
  - Variable types (decimal, character?) - Done
  - Lists - Done
  - Foreach over lists
  - List API to find size, remove first, append lists together etc.
  - Make lists indexed by strings as well
  - Allow functions to return nothing. Detect all code paths return the same value. - done (need to add statement expressions to make this useful though)
  - Static type checking - Done
  - Anonymous functions

- Features that might be interesting to implement but add little to the educational value
  - Add garbage collection and
    - Compile to byte code 
    - Compile to LLVM 
  - Dead variable analysis
  - Unreachable code analysis
  - Constant propagation and expression simplifications


# Syntax/Grammar 
For a formal definitions see the file "grammar"

Here is a simple if statement:
~~~~
input a
if a < 10 then
    output "Larger than 10"
else
    output "Smaller than 10"
end
~~~~

A while loop:
~~~~
val n := 100
while a < 100 do
    output n
    n := n -1
end
~~~~

Lists:
~~~~
val a := []
val n := 0
val nextVal := 0

while nextVal != -1 do
    input nextVal
    a[n] := nextVal
    n := n + 1
end

if n > 0 then
    val i := 0
    while a[i] != -1 do
        output a[i]
        i := i + 1
    end
else
    output "Please enter at least 1 value"
end
~~~~


Functions:
~~~~
function add(a, b) do
    return a + b
end

output add(10, 20)
~~~~


