// Example C file for testing DavaJava migration
#include <stdio.h>
#include <stdlib.h>

typedef struct {
    int x;
    int y;
} Point;

typedef struct {
    Point* points;
    int count;
    char* name;
} Shape;

int add(int a, int b) {
    return a + b;
}

double calculate_area(double width, double height) {
    return width * height;
}

void print_point(Point* p) {
    printf("Point: (%d, %d)\n", p->x, p->y);
}

Point create_point(int x, int y) {
    Point p;
    p.x = x;
    p.y = y;
    return p;
}

int main() {
    Point p1 = create_point(10, 20);
    print_point(&p1);
    
    int sum = add(5, 3);
    double area = calculate_area(10.5, 20.0);
    
    return 0;
}