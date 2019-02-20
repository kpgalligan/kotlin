/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.checker

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.calls.inference.CapturedType
import org.jetbrains.kotlin.resolve.descriptorUtil.hasExactAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.hasNoInferAnnotation
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.types.model.CaptureStatus
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.contains

interface ClassicTypeSystemContext : TypeSystemInferenceExtensionContext {
    override fun TypeConstructorMarker.isDenotable(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return this.isDenotable
    }

    override fun SimpleTypeMarker.withNullability(nullable: Boolean): SimpleTypeMarker {
        require(this is SimpleType, this::errorMessage)
        return this.makeNullableAsSpecified(nullable)
    }

    override fun KotlinTypeMarker.isError(): Boolean {
        require(this is KotlinType, this::errorMessage)
        return this.isError
    }

    override fun SimpleTypeMarker.isStubType(): Boolean {
        assert(this is SimpleType, this::errorMessage)
        return this is StubType
    }

    override fun CapturedTypeMarker.lowerType(): KotlinTypeMarker? {
        require(this is NewCapturedType, this::errorMessage)
        return this.lowerType
    }

    override fun TypeConstructorMarker.isIntersection(): Boolean {
        assert(this is TypeConstructor, this::errorMessage)
        return this is IntersectionTypeConstructor
    }

    override fun identicalArguments(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        require(a is SimpleType, a::errorMessage)
        require(b is SimpleType, b::errorMessage)
        return a.arguments === b.arguments
    }

    override fun KotlinTypeMarker.asSimpleType(): SimpleTypeMarker? {
        require(this is KotlinType, this::errorMessage)
        return this.unwrap() as? SimpleType
    }

    override fun KotlinTypeMarker.asFlexibleType(): FlexibleTypeMarker? {
        require(this is KotlinType, this::errorMessage)
        return this.unwrap() as? FlexibleType
    }

    override fun FlexibleTypeMarker.asDynamicType(): DynamicTypeMarker? {
        assert(this is FlexibleType, this::errorMessage)
        return this as? DynamicType
    }

    override fun FlexibleTypeMarker.asRawType(): RawTypeMarker? {
        assert(this is FlexibleType, this::errorMessage)
        return this as? RawType
    }

    override fun FlexibleTypeMarker.upperBound(): SimpleTypeMarker {
        require(this is FlexibleType, this::errorMessage)
        return this.upperBound
    }

    override fun FlexibleTypeMarker.lowerBound(): SimpleTypeMarker {
        require(this is FlexibleType, this::errorMessage)
        return this.lowerBound
    }

    override fun SimpleTypeMarker.asCapturedType(): CapturedTypeMarker? {
        assert(this is SimpleType, this::errorMessage)
        return this as? NewCapturedType
    }

    override fun SimpleTypeMarker.asDefinitelyNotNullType(): DefinitelyNotNullTypeMarker? {
        assert(this is SimpleType, this::errorMessage)
        return this as? DefinitelyNotNullType
    }

    override fun SimpleTypeMarker.isMarkedNullable(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return this.isMarkedNullable
    }

    override fun SimpleTypeMarker.typeConstructor(): TypeConstructorMarker {
        require(this is SimpleType, this::errorMessage)
        return this.constructor
    }

    override fun SimpleTypeMarker.argumentsCount(): Int {
        require(this is SimpleType, this::errorMessage)
        return this.arguments.size
    }

    override fun SimpleTypeMarker.getArgument(index: Int): TypeArgumentMarker {
        require(this is SimpleType, this::errorMessage)
        return this.arguments[index]
    }

    override fun TypeArgumentMarker.isStarProjection(): Boolean {
        require(this is TypeProjection, this::errorMessage)
        return this.isStarProjection
    }

    override fun TypeArgumentMarker.getVariance(): TypeVariance {
        require(this is TypeProjection, this::errorMessage)
        return this.projectionKind.convertVariance()
    }

    private fun Variance.convertVariance(): TypeVariance {
        return when (this) {
            Variance.INVARIANT -> TypeVariance.INV
            Variance.IN_VARIANCE -> TypeVariance.IN
            Variance.OUT_VARIANCE -> TypeVariance.OUT
        }
    }

    override fun TypeArgumentMarker.getType(): KotlinTypeMarker {
        require(this is TypeProjection, this::errorMessage)
        return this.type.unwrap()
    }


    override fun TypeConstructorMarker.parametersCount(): Int {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters.size
    }

    override fun TypeConstructorMarker.getParameter(index: Int): TypeParameterMarker {
        require(this is TypeConstructor, this::errorMessage)
        return this.parameters[index]
    }

    override fun TypeConstructorMarker.supertypes(): Collection<KotlinTypeMarker> {
        require(this is TypeConstructor, this::errorMessage)
        return this.supertypes
    }

    override fun TypeParameterMarker.getVariance(): TypeVariance {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.variance.convertVariance()
    }

    override fun TypeParameterMarker.upperBoundCount(): Int {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds.size
    }

    override fun TypeParameterMarker.getUpperBound(index: Int): KotlinTypeMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.upperBounds[index]
    }

    override fun TypeParameterMarker.getTypeConstructor(): TypeConstructorMarker {
        require(this is TypeParameterDescriptor, this::errorMessage)
        return this.typeConstructor
    }

    override fun isEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
        assert(c1 is TypeConstructor, c1::errorMessage)
        assert(c2 is TypeConstructor, c2::errorMessage)
        return c1 == c2
    }

    override fun TypeConstructorMarker.isClassTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return declarationDescriptor is ClassDescriptor
    }

    override fun TypeConstructorMarker.isCommonFinalClassConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        val classDescriptor = declarationDescriptor as? ClassDescriptor ?: return false
        return classDescriptor.isFinalClass &&
                classDescriptor.kind != ClassKind.ENUM_ENTRY &&
                classDescriptor.kind != ClassKind.ANNOTATION_CLASS
    }


    override fun TypeArgumentListMarker.get(index: Int): TypeArgumentMarker {
        return when (this) {
            is SimpleTypeMarker -> getArgument(index)
            is ArgumentList -> get(index)
            else -> error("unknown type argument list type: $this, ${this::class}")
        }
    }

    override fun TypeArgumentListMarker.size(): Int {
        return when (this) {
            is SimpleTypeMarker -> argumentsCount()
            is ArgumentList -> size
            else -> error("unknown type argument list type: $this, ${this::class}")
        }
    }

    override fun SimpleTypeMarker.asArgumentList(): TypeArgumentListMarker {
        require(this is SimpleType, this::errorMessage)
        return this
    }

    override fun captureFromArguments(type: SimpleTypeMarker, status: CaptureStatus): SimpleTypeMarker? {
        require(type is SimpleType, type::errorMessage)
        return org.jetbrains.kotlin.types.checker.captureFromArguments(type, status)
    }

    override fun TypeConstructorMarker.isAnyConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.isTypeConstructorForGivenClass(this, FQ_NAMES.any)
    }

    override fun TypeConstructorMarker.isNothingConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.isTypeConstructorForGivenClass(this, FQ_NAMES.nothing)
    }

    override fun KotlinTypeMarker.asTypeArgument(): TypeArgumentMarker {
        require(this is KotlinType, this::errorMessage)
        return this.asTypeProjection()
    }

    override fun TypeConstructorMarker.isUnitTypeConstructor(): Boolean {
        require(this is TypeConstructor, this::errorMessage)
        return KotlinBuiltIns.isTypeConstructorForGivenClass(this, FQ_NAMES.unit)
    }

    /**
     *
     * SingleClassifierType is one of the following types:
     *  - classType
     *  - type for type parameter
     *  - captured type
     *
     * Such types can contains error types in our arguments, but type constructor isn't errorTypeConstructor
     */
    override fun SimpleTypeMarker.isSingleClassifierType(): Boolean {
        require(this is SimpleType, this::errorMessage)
        return !isError &&
                constructor.declarationDescriptor !is TypeAliasDescriptor &&
                (constructor.declarationDescriptor != null || this is CapturedType || this is NewCapturedType || this is DefinitelyNotNullType)
    }

    override fun KotlinTypeMarker.contains(predicate: (KotlinTypeMarker) -> Boolean): Boolean {
        require(this is KotlinType, this::errorMessage)
        return containsInternal(this, predicate)
    }

    override fun SimpleTypeMarker.typeDepth(): Int {
        require(this is SimpleType, this::errorMessage)
        return this.typeDepthInternal()
    }

    override fun KotlinTypeMarker.typeDepth(): Int {
        require(this is UnwrappedType, this::errorMessage)
        return this.typeDepthInternal()
    }

    override fun intersectTypes(types: List<KotlinTypeMarker>): KotlinTypeMarker {
        @Suppress("UNCHECKED_CAST")
        return org.jetbrains.kotlin.types.checker.intersectTypes(types as List<UnwrappedType>)
    }

    override fun Collection<KotlinTypeMarker>.singleBestRepresentative(): KotlinTypeMarker? {
        return singleBestRepresentative(this as Collection<KotlinType>)
    }

    override fun KotlinTypeMarker.isUnit(): Boolean {
        require(this is UnwrappedType)
        return KotlinBuiltIns.isUnit(this)
    }

    override fun createFlexibleType(lowerBound: SimpleTypeMarker, upperBound: SimpleTypeMarker): KotlinTypeMarker {
        require(lowerBound is SimpleType)
        require(upperBound is SimpleType)
        return KotlinTypeFactory.flexibleType(lowerBound, upperBound)
    }

    override fun KotlinTypeMarker.withNullability(nullable: Boolean): KotlinTypeMarker {
        return when (this) {
            is SimpleTypeMarker -> this.withNullability(nullable)
            is FlexibleTypeMarker -> createFlexibleType(lowerBound().withNullability(nullable), upperBound().withNullability(nullable))
            else -> error("sealed")
        }
    }


    override fun newBaseTypeCheckerContext(): AbstractTypeCheckerContext {
        return TypeCheckerContext(false)
    }

    override fun nullableNothingType(): KotlinTypeMarker {
        return builtIns.nullableNothingType
    }

    val builtIns: KotlinBuiltIns get() = throw UnsupportedOperationException("Not supported")

    override fun KotlinTypeMarker.makeDefinitelyNotNullOrNotNull(): KotlinTypeMarker {
        require(this is UnwrappedType)
        return makeDefinitelyNotNullOrNotNullInternal(this)
    }


    override fun SimpleTypeMarker.makeSimpleTypeDefinitelyNotNullOrNotNull(): SimpleTypeMarker {
        require(this is SimpleType)
        return makeSimpleTypeDefinitelyNotNullOrNotNullInternal(this)
    }


    override fun KotlinTypeMarker.removeAnnotations(): KotlinTypeMarker {
        require(this is UnwrappedType)
        return this.replaceAnnotations(Annotations.EMPTY)
    }

    override fun KotlinTypeMarker.hasExactAnnotation(): Boolean {
        require(this is UnwrappedType)
        return hasExactInternal(this)
    }

    override fun KotlinTypeMarker.hasNoInferAnnotation(): Boolean {
        require(this is UnwrappedType)
        return hasNoInferInternal(this)
    }
}

private fun hasNoInferInternal(type: UnwrappedType): Boolean {
    return type.hasNoInferAnnotation()
}


private fun hasExactInternal(type: UnwrappedType): Boolean {
    return type.hasExactAnnotation()
}


private fun makeDefinitelyNotNullOrNotNullInternal(type: UnwrappedType): UnwrappedType {
    return type.makeDefinitelyNotNullOrNotNull()
}

private fun makeSimpleTypeDefinitelyNotNullOrNotNullInternal(type: SimpleType): SimpleType {
    return type.makeSimpleTypeDefinitelyNotNullOrNotNull()
}

private fun containsInternal(type: KotlinType, predicate: (KotlinTypeMarker) -> Boolean): Boolean = type.contains(predicate)

private fun singleBestRepresentative(collection: Collection<KotlinType>) = collection.singleBestRepresentative()

internal fun UnwrappedType.typeDepthInternal() =
    when (this) {
        is SimpleType -> typeDepthInternal()
        is FlexibleType -> Math.max(lowerBound.typeDepthInternal(), upperBound.typeDepthInternal())
    }

internal fun SimpleType.typeDepthInternal(): Int {
    if (this is TypeUtils.SpecialType) return 0

    val maxInArguments = arguments.asSequence().map {
        if (it.isStarProjection) 1 else it.type.unwrap().typeDepthInternal()
    }.max() ?: 0

    return maxInArguments + 1
}


@Suppress("NOTHING_TO_INLINE")
private inline fun Any.errorMessage(): String {
    return "ClassicTypeSystemContext couldn't handle: $this, ${this::class}"
} 