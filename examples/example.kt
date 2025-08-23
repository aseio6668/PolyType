// Example Kotlin file for testing DavaJava migration
package com.example.kotlin

// Data class - showcases Kotlin's concise syntax
data class Person(
    val name: String,
    val age: Int,
    val email: String?
)

// Regular class with properties and methods
class BankAccount(private val accountNumber: String) {
    private var balance: Double = 0.0
    
    fun deposit(amount: Double) {
        if (amount > 0) {
            balance += amount
        }
    }
    
    fun withdraw(amount: Double): Boolean {
        return if (amount > 0 && amount <= balance) {
            balance -= amount
            true
        } else {
            false
        }
    }
    
    fun getBalance(): Double = balance
}

// Extension function - Kotlin-specific feature
fun String.isValidEmail(): Boolean {
    return this.contains("@") && this.contains(".")
}

// Extension property
val Person.displayName: String
    get() = "$name (Age: $age)"

// Top-level function
fun calculateTax(income: Double, rate: Double = 0.15): Double {
    return income * rate
}

// Function with higher-order function parameter
fun processNumbers(numbers: List<Int>, operation: (Int) -> Int): List<Int> {
    return numbers.map(operation)
}

// Class with companion object (similar to static methods)
class MathUtils {
    companion object {
        fun max(a: Int, b: Int): Int = if (a > b) a else b
        
        const val PI = 3.14159
    }
}

// Sealed class for type-safe state management
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Main function
fun main() {
    // Create instances
    val person = Person("John Doe", 30, "john@example.com")
    val account = BankAccount("ACC123")
    
    // Use extension function
    val isValid = person.email?.isValidEmail() ?: false
    
    // Use regular methods
    account.deposit(1000.0)
    val success = account.withdraw(250.0)
    
    // Use top-level function
    val tax = calculateTax(50000.0)
    
    // Use higher-order function
    val numbers = listOf(1, 2, 3, 4, 5)
    val doubled = processNumbers(numbers) { it * 2 }
    
    // Use companion object
    val maxValue = MathUtils.max(10, 20)
    
    println("Person: ${person.displayName}")
    println("Account balance: ${account.getBalance()}")
    println("Tax: $tax")
    println("Doubled numbers: $doubled")
    println("Max value: $maxValue")
}