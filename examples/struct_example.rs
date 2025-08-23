// Example Rust struct for testing DavaJava migration
pub struct Person {
    pub name: String,
    age: i32,
    email: String,
}

struct Point {
    pub x: f64,
    pub y: f64,
}

pub fn create_person(name: String, age: i32, email: String) -> Person {
    Person { name, age, email }
}

fn calculate_distance(p1: Point, p2: Point) -> f64 {
    let dx = p1.x - p2.x;
    let dy = p1.y - p2.y;
    (dx * dx + dy * dy).sqrt()
}