## Session 38: dtype-next workshop planning

We spent time in this session planning and thinking about the 
dtype-next workshop that we will run in November in association
with re:Clojure.

### An initial sketch of topics:

Intro: What is dtype-next?
  - Can be compared to e.g. Numpy
  - But key differences

Central abstraction: Buffers (Reader)
  - Lazy & Non-caching - use this to show the difference between realized and unrealized array.
  - Lazy & Non-caching can be confusing
  - But als/o powerful (e.g. efficient because not creating lots of intermediate structures, can describe array transformations, execute later, what else?)

Array operations (i.e. introduce tech.v3.datatype.functional namespace)

Tensors?
  - Also based on buffers
  - Can use array operations that we use for buffers in many cases
  

## A narrowed outline

Intro
- What is dtype-next?
- Why we use it -> efficiency demo efficiency 

Buffer (aka Reader) & its properties
- Buffers are countable, typed, random access collections of things
- Buffers can be readable and writeable, mostly we work with them as "readers"
- They are lazy and non-caching

Processing & manipulating buffers
- tech.v3.datatype.functional <- key namespace
- Common API signatures? Differ from regular ones? 
- Importance of emap? When to use?
- When does it make sense to use regular clojure operations with buffers?
- Survey common operations
  
Closing/Summary
- Maybe we could leave plenty of time for open discussions & questions

  
