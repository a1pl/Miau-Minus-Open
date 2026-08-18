package com.viaversion.viaversion.util;

import com.google.common.base.Preconditions;

public interface Either<X, Y> {
    static <X, Y> Either<X, Y> left(X left) {
        Preconditions.checkNotNull(left);
        return new EitherImpl<>(left, null);
    }

    static <X, Y> Either<X, Y> right(Y right) {
        Preconditions.checkNotNull(right);
        return new EitherImpl<>(null, right);
    }

    boolean isLeft();

    boolean isRight();

    X left();

    Y right();
}
