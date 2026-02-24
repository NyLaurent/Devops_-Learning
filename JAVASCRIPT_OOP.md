# Object-Oriented Programming in JavaScript

## 1. What is OOP and Why It Exists in JavaScript?

Object-Oriented Programming (OOP) is a programming paradigm that structures software around objects—entities that bundle data (properties) and behavior (methods).

While languages like Java, C#, and C++ were designed with classical OOP from day one, JavaScript took a different path.

### Why OOP Started in JavaScript

When JavaScript was created (in 1995 by Brendan Eich), the web needed:

- **Small scripts**
- **Quick UI manipulation**
- **Dynamic behavior**
- **Reusable components**

JavaScript used prototype-based OOP, inspired by the Self programming language. It didn't originally have classes—only objects that inherit from other objects.

This became the foundation of JavaScript's identity.

---

## 2. Short Summary of OOP in JavaScript

- JavaScript wasn't originally class-based; instead, its OOP uses **prototypes**.
- **ES6 (2015)** added class syntax for easier readability.
- True "classes" in JS = **syntactic sugar**.
- OOP pillars still apply: **Encapsulation, Abstraction, Inheritance, Polymorphism**.
- Modern JS supports real private fields (`#field`).
- Multiple ways exist to build OOP structures.

---

## 3. The Four Pillars of OOP in JavaScript (Complete Chapter)

OOP is built upon four foundational concepts:

1. **Encapsulation**
2. **Abstraction**
3. **Inheritance**
4. **Polymorphism**

### 3.1 Encapsulation

#### Meaning

Encapsulation protects internal object data by only exposing safe, necessary operations. It prevents accidental or malicious modification.

#### Analogy

A car hides engine complexity. You interact with simple controls.

#### Examples

**❌ Without Encapsulation**

```javascript
const user = {
  name: "Stanley",
  password: "12345"
};

user.password = "hacked"; // insecure
```

**✔ Encapsulation Using Private Fields (#)**

```javascript
class User {
  #password;
  
  constructor(name, password) {
    this.name = name;
    this.#password = password;
  }

  checkPassword(p) {
    return this.#password === p;
  }
}
```

**✔ Encapsulation via Closure (Factory Function)**

```javascript
function createAccount(balance = 0) {
  let _balance = balance;

  return {
    deposit(amount) { _balance += amount; },
    getBalance() { return _balance; }
  };
}
```

#### Pros

- ✔ Protects data
- ✔ Clear API
- ✔ Reduces unintended side effects

#### Cons

- ✘ Private fields reduce flexibility
- ✘ Harder to test hidden data

#### Under the Hood

Private fields live in hidden internal slots, not on the object's memory shape.

---

### 3.2 Abstraction

#### Meaning

Exposing only the necessary parts of an object and hiding the internal complexity.

#### Example

```javascript
class BankAccount {
  #balance = 0;

  deposit(amount) {
    this.#balance += amount;
  }

  withdraw(amount) {
    if (amount <= this.#balance) this.#balance -= amount;
  }

  getBalance() {
    return this.#balance;
  }
}
```

Users don't need to know how balance updates work.

#### Pros

- ✔ Simpler interfaces
- ✔ Hides complexity
- ✔ Makes code maintainable

#### Cons

- ✘ Too much hiding can confuse developers

#### JS Engine Insight

Abstraction reduces object shape complexity → shorter prototype chains → faster lookups.

---

### 3.3 Inheritance

#### Meaning

Child objects inherit properties and behavior from parent objects.

#### Class Inheritance

```javascript
class Animal {
  eat() { console.log("Eating"); }
}

class Dog extends Animal {
  bark() { console.log("Bark!"); }
}
```

#### Prototype Inheritance

```javascript
function Animal() {}
Animal.prototype.eat = function() { console.log("Eating"); };

function Dog() {}
Dog.prototype = Object.create(Animal.prototype);
Dog.prototype.bark = function() { console.log("Bark!"); };
```

#### Pros

- ✔ Reusability
- ✔ Less duplication
- ✔ Natural hierarchy

#### Cons

- ✘ Deep inheritance chains = fragile
- ✘ Composition is often better

#### Engine Diagram (Prototype Lookup)

```
dog → Dog.prototype → Animal.prototype → Object.prototype → null
```

---

### 3.4 Polymorphism

#### Meaning

The same method name behaves differently across classes.

#### Example (Overriding)

```javascript
class Animal {
  sound() { console.log("Generic sound"); }
}

class Dog extends Animal {
  sound() { console.log("Bark!"); }
}
```

#### Polymorphic Loop

```javascript
const animals = [new Dog(), new Cat()];
animals.forEach(a => a.sound());
```

#### Pros

- ✔ Flexible
- ✔ Powerful code reuse
- ✔ Enables patterns like Strategy, State, Factory

#### Cons

- ✘ Too much overriding can be confusing

#### Under the Hood

Overridden methods simply shadow parent prototype methods.

---

## 4. All Possible Ways to Create OOP Classes in JS

JavaScript supports numerous OOP styles.

### 4.1 Object Literal Pattern

```javascript
const person = {
  name: "Stanley",
  greet() { console.log("Hello " + this.name); }
};
```

- ✔ Simple
- ❌ No reuse
- ❌ No inheritance

---

### 4.2 Factory Functions

```javascript
function createUser(name) {
  return {
    name,
    greet() { console.log("Hello " + name); }
  };
}
```

- ✔ Easy, supports closure
- ❌ Each instance duplicates methods

---

### 4.3 Constructor Functions

```javascript
function Car(model) {
  this.model = model;
}

Car.prototype.drive = function() {
  console.log("Driving " + this.model);
};
```

- ✔ Memory-efficient
- ❌ Requires `new` keyword (dangerous)
- ❌ No private fields

---

### 4.4 Manual Prototype Pattern

```javascript
const Animal = {
  eat() { console.log("eat"); }
};

const dog = Object.create(Animal);
dog.bark = () => console.log("bark");
```

- ✔ Pure JS OOP
- ❌ Not intuitive for learners

---

### 4.5 ES6 Classes (Syntactic Sugar)

```javascript
class User {
  constructor(name) {
    this.name = name;
  }

  greet() {
    console.log("Hello " + this.name);
  }
}
```

- ✔ Clean, readable
- ❌ Still just prototypes underneath

---

### 4.6 Modern ES2022+ Classes with Private Fields

```javascript
class Account {
  #balance = 0;

  deposit(amount) { this.#balance += amount; }
  getBalance() { return this.#balance; }
}
```

- ✔ True encapsulation
- ✔ Best modern approach
- ❌ Not available in legacy browsers

---

## 5. Comparison Table

| Style | Inheritance | Encapsulation | Memory | Reuse | Complexity |
|-------|-------------|---------------|--------|-------|------------|
| Object Literal | ❌ | ❌ | ❌ | ❌ | ⭐ |
| Factory Function | ❌ | ✔ (closure) | ❌ | ✔ | ⭐⭐ |
| Constructor Function | ✔ | ❌ | ✔✔ | ✔✔ | ⭐⭐⭐ |
| Manual Prototype | ✔ | ❌ | ✔✔✔ | ✔✔ | ⭐⭐⭐⭐ |
| ES6 Class | ✔✔ | ❌ | ✔✔ | ✔✔ | ⭐ |
| ES2022 Class | ✔✔ | ✔✔ | ✔✔ | ✔✔ | ⭐⭐ |

---

## 6. Final Summary

JavaScript is unique:

- It started with **prototypes**, evolved to **constructor functions**, then to **classes**, and finally to **modern private fields**.

The core pillars—**Encapsulation, Abstraction, Inheritance, Polymorphism**—all exist in JavaScript but are implemented in its own flexible and dynamic way.

### JS OOP is powerful because:

- It supports **multiple OOP models**
- It is **dynamic and reflective**
- It blends **functional and OOP programming**
- You can **choose the best style per scenario**

---

## Additional Concepts

### Mixins and Composition

Instead of deep inheritance, JavaScript favors composition:

```javascript
const canFly = {
  fly() { console.log("Flying!"); }
};

const canSwim = {
  swim() { console.log("Swimming!"); }
};

class Duck {
  constructor() {
    Object.assign(this, canFly, canSwim);
  }
}
```

### Static Methods and Properties

```javascript
class MathUtils {
  static PI = 3.14159;
  
  static add(a, b) {
    return a + b;
  }
}

MathUtils.add(1, 2); // 3
```

### Getters and Setters

```javascript
class Temperature {
  constructor(celsius) {
    this._celsius = celsius;
  }

  get fahrenheit() {
    return this._celsius * 9/5 + 32;
  }

  set fahrenheit(value) {
    this._celsius = (value - 32) * 5/9;
  }
}
```

### Symbol-based Privacy (Pre-ES2022)

```javascript
const _balance = Symbol('balance');

class Account {
  constructor() {
    this[_balance] = 0;
  }

  getBalance() {
    return this[_balance];
  }
}
```

### Prototype Chain Visualization

```javascript
// Understanding the prototype chain
function Parent() {}
Parent.prototype.parentMethod = function() {};

function Child() {}
Child.prototype = Object.create(Parent.prototype);
Child.prototype.constructor = Child;

const child = new Child();
// child → Child.prototype → Parent.prototype → Object.prototype → null
```

### The `this` Keyword and Method Binding

Understanding `this` is crucial for JavaScript OOP:

```javascript
class Button {
  constructor(text) {
    this.text = text;
  }

  click() {
    console.log(`${this.text} clicked`);
  }
}

const btn = new Button("Submit");
btn.click(); // "Submit clicked"

// Problem: losing 'this' context
const clickHandler = btn.click;
clickHandler(); // undefined clicked (this is lost)

// Solutions:
// 1. Bind
const boundClick = btn.click.bind(btn);
boundClick(); // "Submit clicked"

// 2. Arrow functions (lexical this)
class Button {
  constructor(text) {
    this.text = text;
    this.click = () => console.log(`${this.text} clicked`);
  }
}

// 3. Class field arrow functions (ES2022)
class Button {
  text = "";
  click = () => console.log(`${this.text} clicked`);
}
```

### The `super` Keyword

`super` allows access to parent class methods and constructors:

```javascript
class Animal {
  constructor(name) {
    this.name = name;
  }

  speak() {
    console.log(`${this.name} makes a sound`);
  }
}

class Dog extends Animal {
  constructor(name, breed) {
    super(name); // Call parent constructor
    this.breed = breed;
  }

  speak() {
    super.speak(); // Call parent method
    console.log(`${this.name} barks!`);
  }
}
```

### Method Chaining (Fluent Interface)

Return `this` to enable method chaining:

```javascript
class Calculator {
  constructor(value = 0) {
    this.value = value;
  }

  add(n) {
    this.value += n;
    return this; // Enable chaining
  }

  multiply(n) {
    this.value *= n;
    return this;
  }

  getValue() {
    return this.value;
  }
}

const calc = new Calculator(10);
calc.add(5).multiply(2).add(3); // 10 + 5 = 15, * 2 = 30, + 3 = 33
```

### Object Immutability

Control object mutability with built-in methods:

```javascript
const obj = { name: "Stanley", age: 30 };

// Object.preventExtensions() - Can't add new properties
Object.preventExtensions(obj);
obj.city = "NYC"; // Fails silently in non-strict mode

// Object.seal() - Can't add/remove properties, but can modify
Object.seal(obj);
delete obj.name; // Fails
obj.age = 31; // Works

// Object.freeze() - Complete immutability
Object.freeze(obj);
obj.age = 32; // Fails
```

### Property Descriptors

Control property behavior with descriptors:

```javascript
const obj = {};

Object.defineProperty(obj, 'readOnly', {
  value: 42,
  writable: false,
  enumerable: true,
  configurable: false
});

obj.readOnly = 100; // Fails silently
console.log(obj.readOnly); // 42

// Get descriptor
const descriptor = Object.getOwnPropertyDescriptor(obj, 'readOnly');
```

### Computed Properties

Use expressions as property names:

```javascript
const propName = 'dynamic';
const obj = {
  [propName]: 'value',
  [`${propName}Key`]: 'another value',
  [Symbol('id')]: 123
};

class MyClass {
  [Symbol.iterator]() {
    // Makes class iterable
  }
}
```

### Type Checking: `instanceof` and `constructor`

```javascript
class Animal {}
class Dog extends Animal {}

const dog = new Dog();

console.log(dog instanceof Dog);      // true
console.log(dog instanceof Animal);   // true
console.log(dog instanceof Object);  // true

console.log(dog.constructor === Dog); // true

// Duck typing (check for methods instead of type)
function canFly(obj) {
  return typeof obj.fly === 'function';
}
```

### Abstract Classes (Simulation)

JavaScript doesn't have abstract classes, but you can simulate them:

```javascript
class AbstractShape {
  constructor() {
    if (this.constructor === AbstractShape) {
      throw new Error("Cannot instantiate abstract class");
    }
    if (!this.calculateArea) {
      throw new Error("Must implement calculateArea");
    }
  }
}

class Circle extends AbstractShape {
  constructor(radius) {
    super();
    this.radius = radius;
  }

  calculateArea() {
    return Math.PI * this.radius ** 2;
  }
}
```

### Proxy and Reflect (Advanced)

Intercept and customize object operations:

```javascript
const handler = {
  get(target, prop) {
    if (prop === 'age') {
      return target[prop] || 'Unknown';
    }
    return Reflect.get(target, prop);
  },
  
  set(target, prop, value) {
    if (prop === 'age' && value < 0) {
      throw new Error('Age cannot be negative');
    }
    return Reflect.set(target, prop, value);
  }
};

const person = new Proxy({ name: 'Stanley' }, handler);
person.age = 25;
console.log(person.age); // 25
person.age = -5; // Error
```

### Object Destructuring and Spread

Modern JavaScript features for object manipulation:

```javascript
class User {
  constructor(name, email, age) {
    this.name = name;
    this.email = email;
    this.age = age;
  }

  // Destructuring in methods
  update({ name, email }) {
    if (name) this.name = name;
    if (email) this.email = email;
  }

  // Return destructured object
  getPublicInfo() {
    const { name, email } = this;
    return { name, email };
  }
}

// Spread operator for cloning and merging
const user1 = new User('Stanley', 's@example.com', 30);
const user2 = { ...user1, name: 'John' }; // Shallow copy
```

### Common Design Patterns

#### Singleton Pattern

```javascript
class Database {
  static instance = null;

  constructor() {
    if (Database.instance) {
      return Database.instance;
    }
    Database.instance = this;
  }
}

const db1 = new Database();
const db2 = new Database();
console.log(db1 === db2); // true
```

#### Factory Pattern

```javascript
class AnimalFactory {
  static create(type, name) {
    switch(type) {
      case 'dog':
        return new Dog(name);
      case 'cat':
        return new Cat(name);
      default:
        throw new Error('Unknown animal type');
    }
  }
}
```

#### Observer Pattern

```javascript
class EventEmitter {
  constructor() {
    this.events = {};
  }

  on(event, callback) {
    if (!this.events[event]) {
      this.events[event] = [];
    }
    this.events[event].push(callback);
  }

  emit(event, data) {
    if (this.events[event]) {
      this.events[event].forEach(callback => callback(data));
    }
  }
}
```

### Async Methods in Classes

```javascript
class ApiClient {
  async fetchUser(id) {
    const response = await fetch(`/api/users/${id}`);
    return response.json();
  }

  async *fetchUsers() { // Async generator
    let page = 1;
    while (true) {
      const users = await this.fetchPage(page);
      if (users.length === 0) break;
      yield* users;
      page++;
    }
  }
}
```

### Error Handling in OOP

```javascript
class BankAccount {
  #balance = 0;

  withdraw(amount) {
    if (amount <= 0) {
      throw new Error('Amount must be positive');
    }
    if (amount > this.#balance) {
      throw new InsufficientFundsError(this.#balance, amount);
    }
    this.#balance -= amount;
  }
}

class InsufficientFundsError extends Error {
  constructor(balance, requested) {
    super(`Insufficient funds: ${balance} < ${requested}`);
    this.balance = balance;
    this.requested = requested;
  }
}
```

### Performance Considerations

```javascript
// ❌ Slow: Creating methods in constructor
class Slow {
  constructor() {
    this.method = function() { /* ... */ };
  }
}

// ✔ Fast: Methods on prototype (automatic with classes)
class Fast {
  method() { /* ... */ }
}

// Memory: Private fields vs closures
class WithPrivate {
  #data = [];
  // Each instance has its own #data
}

function withClosure() {
  const data = []; // Each instance has its own closure
  return { /* ... */ };
}
```

### Prototype Pollution (Security)

Be aware of prototype pollution vulnerabilities:

```javascript
// Dangerous: Modifying Object.prototype
Object.prototype.isAdmin = true; // Affects ALL objects!

// Safe: Use Object.create(null) for data objects
const safeObj = Object.create(null);
safeObj.isAdmin = true; // Only affects this object

// Safe: Use Map for key-value storage
const safeMap = new Map();
safeMap.set('isAdmin', true);
```

### Class Fields and Static Initialization Blocks

Modern class field syntax and static initialization:

```javascript
class MyClass {
  // Public field
  publicField = 'value';
  
  // Private field
  #privateField = 'hidden';
  
  // Static field
  static staticField = 'shared';
  
  // Static private field
  static #staticPrivate = 'secret';
  
  // Static initialization block (runs once)
  static {
    console.log('Class initialized');
    // Can do complex initialization here
  }
  
  // Method
  method() {
    return this.#privateField;
  }
}
```

### Iterators and Generators

Make classes iterable:

```javascript
class NumberRange {
  constructor(start, end) {
    this.start = start;
    this.end = end;
  }

  // Make class iterable
  *[Symbol.iterator]() {
    for (let i = this.start; i <= this.end; i++) {
      yield i;
    }
  }
}

const range = new NumberRange(1, 5);
for (const num of range) {
  console.log(num); // 1, 2, 3, 4, 5
}

// Generator methods
class Fibonacci {
  *generate(count) {
    let [a, b] = [0, 1];
    for (let i = 0; i < count; i++) {
      yield a;
      [a, b] = [b, a + b];
    }
  }
}
```

### Object.assign() vs Spread Operator

Different ways to copy and merge objects:

```javascript
class User {
  constructor(data) {
    // Object.assign
    Object.assign(this, { name: '', email: '' }, data);
    
    // Or with spread (more modern)
    // this = { name: '', email: '', ...data };
  }
  
  clone() {
    // Shallow copy
    return Object.assign({}, this);
    // Or: return { ...this };
  }
  
  merge(other) {
    return { ...this, ...other };
  }
}
```

### Object.keys(), Object.values(), Object.entries()

Iterate over object properties:

```javascript
class Product {
  constructor(name, price, category) {
    this.name = name;
    this.price = price;
    this.category = category;
  }
  
  // Get all property names
  getKeys() {
    return Object.keys(this);
  }
  
  // Get all values
  getValues() {
    return Object.values(this);
  }
  
  // Get key-value pairs
  getEntries() {
    return Object.entries(this);
  }
  
  // Convert to Map
  toMap() {
    return new Map(Object.entries(this));
  }
}
```

### hasOwnProperty() vs in Operator

Check property existence:

```javascript
class Example {
  ownProp = 'value';
}

Example.prototype.inheritedProp = 'inherited';

const obj = new Example();

console.log(obj.hasOwnProperty('ownProp'));        // true
console.log(obj.hasOwnProperty('inheritedProp'));  // false
console.log('inheritedProp' in obj);               // true

// Modern alternative (safer)
console.log(Object.hasOwn(obj, 'ownProp'));        // true (ES2022)
```

### Object.getPrototypeOf() and Object.setPrototypeOf()

Manipulate prototype chain:

```javascript
const animal = { eat() { console.log('Eating'); } };
const dog = { bark() { console.log('Bark!'); } };

// Set prototype (not recommended for performance)
Object.setPrototypeOf(dog, animal);
dog.eat(); // "Eating"

// Get prototype
const proto = Object.getPrototypeOf(dog);
console.log(proto === animal); // true

// Check if object is prototype of another
console.log(animal.isPrototypeOf(dog)); // true
```

### Class Expressions

Classes can be defined as expressions:

```javascript
// Named class expression
const User = class UserClass {
  constructor(name) {
    this.name = name;
  }
};

// Anonymous class expression
const createUser = class {
  constructor(name) {
    this.name = name;
  }
};

// Immediately invoked class expression
const instance = new (class {
  constructor(value) {
    this.value = value;
  }
})(42);
```

### Multiple Inheritance Simulation

JavaScript doesn't support multiple inheritance, but you can simulate it:

```javascript
// Mixin pattern
const CanFly = {
  fly() {
    console.log(`${this.name} is flying`);
  }
};

const CanSwim = {
  swim() {
    console.log(`${this.name} is swimming`);
  }
};

class Duck {
  constructor(name) {
    this.name = name;
  }
}

// Apply mixins
Object.assign(Duck.prototype, CanFly, CanSwim);

const duck = new Duck('Donald');
duck.fly();  // "Donald is flying"
duck.swim(); // "Donald is swimming"
```

### Method Overloading Simulation

JavaScript doesn't have method overloading, but you can simulate it:

```javascript
class Calculator {
  add(...args) {
    if (args.length === 1 && typeof args[0] === 'number') {
      return args[0] + 10; // Single number
    } else if (args.length === 2) {
      return args[0] + args[1]; // Two numbers
    } else if (Array.isArray(args[0])) {
      return args[0].reduce((a, b) => a + b, 0); // Array
    }
    throw new Error('Invalid arguments');
  }
}
```

---

## Best Practices

1. **Prefer Composition over Inheritance**: Use mixins and composition for flexibility
2. **Use Private Fields**: Leverage `#` for true encapsulation in modern code
3. **Keep Inheritance Shallow**: Avoid deep inheritance chains
4. **Choose the Right Pattern**: Match the OOP style to your use case
5. **Understand Prototypes**: Even when using classes, understanding prototypes helps debug issues

---

## Resources

- [MDN: Classes](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Classes)
- [MDN: Inheritance and the prototype chain](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Inheritance_and_the_prototype_chain)
- [ECMAScript Specification](https://tc39.es/ecma262/)

