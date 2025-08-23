# Simple Tkinter GUI application example
import tkinter as tk
from tkinter import messagebox

class SimpleGUI:
    def __init__(self):
        self.root = tk.Tk()
        self.root.title("Simple Calculator")
        self.root.geometry("300x400")
        
        self.result_var = tk.StringVar()
        self.create_widgets()
        
    def create_widgets(self):
        # Display
        display = tk.Entry(self.root, textvariable=self.result_var, font=("Arial", 14), justify="right")
        display.pack(pady=10, padx=10, fill=tk.X)
        
        # Button frame
        button_frame = tk.Frame(self.root)
        button_frame.pack(pady=10)
        
        # Number buttons
        for i in range(10):
            btn = tk.Button(button_frame, text=str(i), command=lambda x=i: self.number_click(x))
            row = (9-i) // 3
            col = (9-i) % 3
            btn.grid(row=row, column=col, padx=2, pady=2)
        
        # Operation buttons
        plus_btn = tk.Button(button_frame, text="+", command=lambda: self.operation_click("+"))
        plus_btn.grid(row=0, column=3, padx=2, pady=2)
        
        equals_btn = tk.Button(button_frame, text="=", command=self.calculate)
        equals_btn.grid(row=1, column=3, padx=2, pady=2)
        
        clear_btn = tk.Button(button_frame, text="C", command=self.clear)
        clear_btn.grid(row=2, column=3, padx=2, pady=2)
        
    def number_click(self, num):
        current = self.result_var.get()
        self.result_var.set(current + str(num))
        
    def operation_click(self, op):
        current = self.result_var.get()
        self.result_var.set(current + " " + op + " ")
        
    def calculate(self):
        try:
            expression = self.result_var.get()
            result = eval(expression)
            self.result_var.set(str(result))
        except:
            messagebox.showerror("Error", "Invalid expression")
            
    def clear(self):
        self.result_var.set("")
        
    def run(self):
        self.root.mainloop()

def main():
    app = SimpleGUI()
    app.run()

if __name__ == "__main__":
    main()