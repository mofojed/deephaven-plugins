package io.deephaven.ui

import io.deephaven.ui.util.PropCase
import spock.lang.Specification

class PropCaseSpec extends Specification {

    def "converts snake_case to camelCase"() {
        expect:
        PropCase.toCamelCase(input) == expected

        where:
        input             || expected
        'on_press'        || 'onPress'
        'is_disabled'     || 'isDisabled'
        'flex_grow'       || 'flexGrow'
        'value'           || 'value'
        'min_width'       || 'minWidth'
    }

    def "leaves camelCase keys unchanged"() {
        expect:
        PropCase.toCamelCase('onPress') == 'onPress'
        PropCase.toCamelCase('isDisabled') == 'isDisabled'
    }

    def "preserves UNSAFE_ prefix"() {
        expect:
        PropCase.toReactPropCase('UNSAFE_class_name') == 'UNSAFE_className'
        PropCase.toReactPropCase('UNSAFE_style') == 'UNSAFE_style'
    }

    def "converts aria_ to aria-"() {
        expect:
        PropCase.toReactPropCase('aria_label') == 'aria-label'
        PropCase.toReactPropCase('aria_labelled_by') == 'aria-labelledBy'
    }

    def "dictToReactProps drops null values and converts keys"() {
        when:
        def out = PropCase.dictToReactProps([
                on_press: 'cb',
                is_disabled: null,
                aria_label: 'hi',
                UNSAFE_class_name: 'foo',
                value: 42
        ])

        then:
        out == [onPress: 'cb', 'aria-label': 'hi', UNSAFE_className: 'foo', value: 42]
    }
}
