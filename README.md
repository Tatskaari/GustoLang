# GustoLang 
[![Build Status](https://travis-ci.org/Tatskaari/GustoLang.svg?branch=master)](https://travis-ci.org/Tatskaari/GustoLang)
[![codecov](https://codecov.io/gh/Tatskaari/KotlinLang/branch/master/graph/badge.svg)](https://codecov.io/gh/Tatskaari/KotlinLang)

A toy language written in kotlin. The original idea was to make a language that was simple to pick up for beginners
however I have just been implementing features for fun. The plan right now is to get this targeting the JVM.

Another goal of this project is to try out a few idioms. You may have noticed the build passing tag. Every commit to 
this project is run through Travis CI and passed through a suite of unit tests. I also just wanted to learn Kotlin.

# Road Map
- Desired syntax and semantics
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
    - Length - Done
    - First
    - Last
    - Remove
    - Append
  - Make lists indexed by strings and other variables as well
  - Allow functions to return nothing. Detect all code paths return the same value. - Started (naive implementation)
  - Static type checking - Done
  - Anonymous functions - Done
- Interpretation
    - jar command line interpreter - Done
    - Javascript interpreter - Done
- Compilation
    - JVM

- Features to consider
  - Target LLVM IR (and as a result all the platforms LLVM targets)
    - Figure out how to work with LLVM garbage collectors
  - Dead variable analysis
  - Unreachable code analysis
  - Constant propagation and expression simplifications
  - Function inlining


# Syntax/Grammar 
The syntax changes every time I have a cool idea. I keep a formal definition in the file "grammar" however I forget to 
update this sometimes. I usually keep a working example on [my website](http://jon-poole.uk/try-gusto).



