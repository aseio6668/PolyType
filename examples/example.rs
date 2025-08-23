// Example Rust file for testing DavaJava migration
pub fn calculate_sum(a: i32, b: i32) -> i32 {
    a + b
}

fn private_helper(data: &str) -> bool {
    !data.is_empty()
}

pub fn process_vector(mut numbers: Vec<i32>) -> i32 {
    numbers.sort();
    numbers.iter().sum()
}

fn fibonacci(n: u32) -> u32 {
    match n {
        0 => 0,
        1 => 1,
        _ => fibonacci(n - 1) + fibonacci(n - 2),
    }
}