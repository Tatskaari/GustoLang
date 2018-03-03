# GustoLang 
[![Build Status](https://travis-ci.org/Tatskaari/GustoLang.svg?branch=master)](https://travis-ci.org/Tatskaari/GustoLang)
[![codecov](https://codecov.io/gh/Tatskaari/GustoLang/branch/master/graph/badge.svg)](https://codecov.io/gh/Tatskaari/GustoLang)

A toy language written in kotlin. The original idea was to make a language that was simple to pick up for beginners
however I have just been implementing features for fun. The plan right now is to get this targeting the JVM.

Another goal of this project is to try out a few idioms. You may have noticed the build passing tag. Every commit to 
this project is run through Travis CI and passed through a suite of unit tests. I also just wanted to learn Kotlin.

#Road map
I hope to make the following possible in gusto:
```
type expression is
    Add of (expr, expr),
    Sub of (expr, expr),
    Mul of (expr, expr),
    Div of (expr, expr),
    IntLit of integer,
    NumList of integer
end
    
type exprType is
    Int, Num
end

trait eval is
    function eval() : numeric,
    function getType() : exprType
end
    
implement eval for expression as
    function eval() do
        return match this with
            Add(lhs, rhs) -> lhs.eval() + rhs.eval()
            Sub(lhs, rhs) -> lhs.eval() - rhs.eval()
            Div(lhs, rhs) -> lhs.eval() / rhs.eval()
            Mul(lhs, rhs) -> lhs.eval() * rhs.eval()
            IntLit(value) -> value
            NumList(value) -> value
        end
    end
    
    function getType() do
        return match this with 
            Add(lhs, rhs), Sub(lhs, rhs), Mul(lhs, rhs), Div(lhs, rhs) -> do
                val lhsType = lhs.getType()
                val rhsType = rhs.getType()
                if lhsType = Int and rhsType = Int then
                    return Int
                else
                    return Num
                end
            end
            IntLit(value) -> Int
            NumList(value) -> Num
        end
    end
end
```

Which would require the following

- Enumerated types - Done
- Tuples - Done
- Type infererence - Basic type only
- Match statements - Done
- Pattern matching - Done
- Traits / type classes
- Function overloading


# Syntax/grammar 
The syntax changes every time I have a cool idea. I keep a formal definition in the file "grammar" however I forget to 
update this sometimes. I usually keep a working example on [my website](http://jon-poole.uk/try-gusto).



