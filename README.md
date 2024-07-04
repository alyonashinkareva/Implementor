# Implementor
Implementor class, which generates implementations of Java classes and interfaces.
The class implements JarImpler interface.
* Command line argument: the fully qualified name of the class/interface for which you want to generate an implementation.
* As a result of the work, java code for a class with the Impl suffix should be generated, extending (implementing) the specified class (interface).
* The generated class should compile without errors.
* The generated class must not be abstract.
* Methods of the generated class should ignore their arguments and return default values.