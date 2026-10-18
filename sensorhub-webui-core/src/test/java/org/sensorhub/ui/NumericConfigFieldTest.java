/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
the specific language governing rights and limitations under the License.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Locale;

import org.junit.Test;
import org.sensorhub.api.config.DisplayInfo.ValueRange;
import org.sensorhub.ui.data.FieldProperty;

import com.vaadin.v7.data.Validator;
import com.vaadin.v7.data.Validator.InvalidValueException;
import com.vaadin.v7.data.util.converter.Converter;
import com.vaadin.v7.data.util.converter.Converter.ConversionException;
import com.vaadin.v7.data.util.converter.ConverterFactory;
import com.vaadin.v7.data.validator.IntegerRangeValidator;
import com.vaadin.v7.data.validator.LongRangeValidator;
import com.vaadin.v7.data.validator.ShortRangeValidator;


public class NumericConfigFieldTest
{
    static final long TWO_GIB = 2L * 1024 * 1024 * 1024;


    public static class LongConfig
    {
        @ValueRange(min = 1, max = 4L * 1024 * 1024 * 1024)
        public long maximumBytes = TWO_GIB;
    }


    @Test
    public void wholeNumberConvertersPreserveTheirModelWidths()
    {
        ConverterFactory factory = AdminUI.createConfigConverterFactory();
        Converter<String, Long> longConverter = factory.createConverter(String.class, Long.class);
        Converter<String, Integer> integerConverter = factory.createConverter(String.class, Integer.class);
        Converter<String, Short> shortConverter = factory.createConverter(String.class, Short.class);

        assertEquals(Long.class, longConverter.getModelType());
        assertEquals(Long.valueOf(TWO_GIB),
                longConverter.convertToModel(Long.toString(TWO_GIB), Long.class, Locale.FRANCE));
        assertEquals(Long.toString(TWO_GIB),
                longConverter.convertToPresentation(TWO_GIB, String.class, Locale.FRANCE));
        assertEquals(Long.valueOf(Long.MAX_VALUE),
                longConverter.convertToModel(Long.toString(Long.MAX_VALUE), Long.class, Locale.US));

        assertEquals(Integer.valueOf(Integer.MAX_VALUE), integerConverter.convertToModel(
                Integer.toString(Integer.MAX_VALUE), Integer.class, Locale.US));
        assertThrows(ConversionException.class, () -> integerConverter.convertToModel(
                Long.toString(TWO_GIB), Integer.class, Locale.US));

        assertEquals(Short.valueOf(Short.MAX_VALUE), shortConverter.convertToModel(
                Short.toString(Short.MAX_VALUE), Short.class, Locale.US));
        assertThrows(ConversionException.class, () -> shortConverter.convertToModel(
                Integer.toString(Short.MAX_VALUE + 1), Short.class, Locale.US));
    }


    @Test
    public void longConfigPropertyAndRangeAcceptTwoGiB() throws Exception
    {
        LongConfig config = new LongConfig();
        Field field = LongConfig.class.getField("maximumBytes");
        FieldProperty property = new FieldProperty(config, field);
        ValueRange range = field.getAnnotation(ValueRange.class);

        assertEquals(Long.class, property.getType());
        assertEquals(Long.valueOf(TWO_GIB), property.getValue());

        Validator validator = GenericConfigForm.createNumericRangeValidator(
                property.getType(), "outside range", range.min(), range.max());
        assertTrue(validator instanceof LongRangeValidator);
        validator.validate(TWO_GIB);
        validator.validate(range.max());
        assertThrows(InvalidValueException.class, () -> validator.validate(range.max() + 1));

        property.setValue(range.max());
        assertEquals(range.max(), config.maximumBytes);
    }


    @Test
    public void integralRangeValidatorsMatchTheConfigurationPropertyType()
    {
        Validator integerValidator = GenericConfigForm.createNumericRangeValidator(
                Integer.class, "outside range", Long.MIN_VALUE, Long.MAX_VALUE);
        Validator shortValidator = GenericConfigForm.createNumericRangeValidator(
                Short.class, "outside range", Long.MIN_VALUE, Long.MAX_VALUE);

        assertTrue(integerValidator instanceof IntegerRangeValidator);
        integerValidator.validate(Integer.MIN_VALUE);
        integerValidator.validate(Integer.MAX_VALUE);
        assertTrue(shortValidator instanceof ShortRangeValidator);
        shortValidator.validate(Short.MIN_VALUE);
        shortValidator.validate(Short.MAX_VALUE);
    }
}
