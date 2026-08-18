package com.viaversion.viaversion.util;

import com.google.common.base.Preconditions;
import java.util.Objects;

public class EitherImpl<X, Y> implements Either<X, Y> {
    private final X left;
    private final Y right;

    protected EitherImpl(X left, Y value) {
        this.left = left;
        this.right = value;
        Preconditions.checkArgument(left == null || value == null, "Either.left and Either.right are both present");
        Preconditions.checkArgument(left != null || value != null, "Either.left and Either.right are both null");
    }

    @Override
    public boolean isLeft() {
        return this.left != null;
    }

    @Override
    public boolean isRight() {
        return this.right != null;
    }

    @Override
    public X left() {
        return this.left;
    }

    @Override
    public Y right() {
        return this.right;
    }

    @Override
    public String toString() {
        return "Either{" + this.left + ", " + this.right + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            EitherImpl<?, ?> pair = (EitherImpl<?, ?>)o;
            return !Objects.equals(this.left, pair.left) ? false : Objects.equals(this.right, pair.right);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = this.left != null ? this.left.hashCode() : 0;
        return 31 * result + (this.right != null ? this.right.hashCode() : 0);
    }
}
