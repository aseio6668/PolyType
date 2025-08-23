// Example C++ file for testing DavaJava migration
#include <iostream>
#include <vector>
#include <string>
#include <memory>

using namespace std;

class Point {
private:
    double x, y;

public:
    Point(double x = 0, double y = 0) : x(x), y(y) {
        // Constructor
    }
    
    double getX() const { return x; }
    double getY() const { return y; }
    
    void setX(double newX) { x = newX; }
    void setY(double newY) { y = newY; }
    
    double distance(const Point& other) const {
        double dx = x - other.x;
        double dy = y - other.y;
        return sqrt(dx * dx + dy * dy);
    }
};

class Shape {
protected:
    string name;
    vector<Point> points;

public:
    Shape(const string& name) : name(name) {}
    
    virtual ~Shape() = default;
    
    void addPoint(const Point& p) {
        points.push_back(p);
    }
    
    virtual double area() const = 0;
    virtual double perimeter() const = 0;
    
    const string& getName() const { return name; }
    size_t getPointCount() const { return points.size(); }
};

class Rectangle : public Shape {
private:
    double width, height;

public:
    Rectangle(double w, double h) : Shape("Rectangle"), width(w), height(h) {
        addPoint(Point(0, 0));
        addPoint(Point(w, 0));
        addPoint(Point(w, h));
        addPoint(Point(0, h));
    }
    
    double area() const override {
        return width * height;
    }
    
    double perimeter() const override {
        return 2 * (width + height);
    }
};

// Utility functions
double calculateTotalArea(const vector<shared_ptr<Shape>>& shapes) {
    double total = 0.0;
    for (const auto& shape : shapes) {
        total += shape->area();
    }
    return total;
}

void printShapeInfo(const Shape& shape) {
    cout << "Shape: " << shape.getName() 
         << ", Points: " << shape.getPointCount()
         << ", Area: " << shape.area()
         << ", Perimeter: " << shape.perimeter() << endl;
}

int main() {
    vector<shared_ptr<Shape>> shapes;
    
    shapes.push_back(make_shared<Rectangle>(10.0, 5.0));
    shapes.push_back(make_shared<Rectangle>(3.0, 4.0));
    
    cout << "Shape Information:" << endl;
    for (const auto& shape : shapes) {
        printShapeInfo(*shape);
    }
    
    cout << "Total area: " << calculateTotalArea(shapes) << endl;
    
    return 0;
}