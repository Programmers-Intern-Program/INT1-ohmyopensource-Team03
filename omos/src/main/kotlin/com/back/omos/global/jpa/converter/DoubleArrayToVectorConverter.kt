package com.back.omos.global.jpa.converter

import com.pgvector.PGvector
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * Kotlin의 [DoubleArray]와 PostgreSQL의 `vector` 타입을 매핑하는 JPA 컨버터입니다.
 * 
 * <p>
 * `com.pgvector:pgvector` 라이브러리의 [PGvector] 클래스를 사용하여 
 * DB 입출력 시 데이터 형식을 변환합니다.
 *
 * @author MintyU
 * @since 2026-04-22
 */
@Converter
class DoubleArrayToVectorConverter : AttributeConverter<DoubleArray, String> {

    /**
     * [DoubleArray]를 DB의 `vector` 형식(String)으로 변환합니다.
     */
    override fun convertToDatabaseColumn(attribute: DoubleArray?): String? {
        if (attribute == null) return null
        // PGvector는 float[]을 인자로 받습니다.
        val floatArray = attribute.map { it.toFloat() }.toFloatArray()
        return PGvector(floatArray).toString()
    }

    /**
     * DB의 `vector` 데이터(String)를 [DoubleArray]로 변환합니다.
     */
    override fun convertToEntityAttribute(dbData: String?): DoubleArray? {
        if (dbData == null) return null
        // PGvector로 파싱 후 float[]을 추출하여 DoubleArray로 변환합니다.
        val pgVector = PGvector(dbData)
        return pgVector.toArray().map { it.toDouble() }.toDoubleArray()
    }
}
