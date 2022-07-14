# The FHIR Resources needed for the scenario

In here, we provide the FHIR resources, that were generated for execution of the medical guideline, having already existing data available, that the execution engine can make use of.

## The mild to moderate ill patient
This defines for the initial assessment the following attributes:
|  Attribute           |   LOINC Code     |     value            |
| ---                  | ---              | ---             |
|speaksFine            |46679-7           | unlimited|
|cyanosis              |39107-8           | Normal|
|psychomotor_activity  |80288-4           | alert|

this is named in the samples as "_mild"

## The severe ill patient
This defines for the initial assessment the following attributes:
|  Attribute           |   LOINC Code     |     value            |
| ---                  | ---              | ---             |
|speaksFine            |46679-7           | Limited
|cyanosis              |39107-8           | value: Pale
|psychomotor_activity  |80288-4           | Stuporous

this is named in the samples as "_moderate"

## The life-threatening ill patient
This defines for the initial assessment the following attributes:
|  Attribute           |   LOINC Code     |     value            |
| ---                  | ---              | ---             |
|speaksFine            |46679-7           |  Limited
|cyanosis              |39107-8           |  Cyanotic
|psychomotor_activity  |80288-4           |  Confused

this is named in the samples as "_severe"
