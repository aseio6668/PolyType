// Example C# file for testing DavaJava migration
using System;
using System.Collections.Generic;
using System.Linq;

namespace Example
{
    public class Calculator
    {
        private int _value;

        public Calculator(int initialValue = 0)
        {
            _value = initialValue;
        }

        public int Add(int amount)
        {
            _value += amount;
            return _value;
        }

        public int GetValue()
        {
            return _value;
        }

        public static int CalculateSum(int a, int b)
        {
            return a + b;
        }

        public static int ProcessList(List<int> numbers)
        {
            numbers.Sort();
            return numbers.Sum();
        }
    }
}