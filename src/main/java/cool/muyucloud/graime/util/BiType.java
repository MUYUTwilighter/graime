package cool.muyucloud.graime.util;

import com.google.gson.JsonElement;

import javax.lang.model.type.PrimitiveType;
import java.util.Objects;

public class BiType<A, B> {
    A a;
    B b;

    public BiType(A a, B b) {
        this.a = a;
        this.b = b;
    }


    public A getA() {
        return a;
    }

    public void setA(A a) {
        this.a = a;
    }

    public B getB() {
        return b;
    }

    public void setB(B b) {
        this.b = b;
    }

    @Override
    public String toString() {
        return "BiType{" + a + ", " + b + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiType<?, ?> biType = (BiType<?, ?>) o;
        return Objects.equals(a, biType.a) && Objects.equals(b, biType.b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b);
    }
}
