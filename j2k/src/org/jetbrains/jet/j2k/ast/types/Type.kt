/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.j2k.ast.types

import org.jetbrains.jet.j2k.ast.Element
import org.jetbrains.jet.j2k.Converter

public fun Type.isPrimitive(): Boolean = this is PrimitiveType
public fun Type.isUnit(): Boolean = this == UnitType

public abstract class MayBeNullableType(nullable: Boolean, val converter: Converter): Type {
    override public val nullable: Boolean = !converter.settings.forceNotNullTypes && nullable
}

public trait NotNullType : Type {
    override public val nullable: Boolean
        get() = false
}

public object UnitType: NotNullType {
    override fun toKotlin() = "Unit"
}

public trait Type : Element {

    public val nullable: Boolean

    public open fun convertedToNotNull(): Type {
        if (nullable) throw UnsupportedOperationException("convertedToNotNull must be defined")
        return this
    }

    protected fun isNullableStr(): String? {
        return if (nullable) "?" else ""
    }
}
