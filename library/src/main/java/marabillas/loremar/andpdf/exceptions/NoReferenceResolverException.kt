package marabillas.loremar.andpdf.exceptions

class NoReferenceResolverException : Exception(
    "ReferenceResolver is not set. Call ObjectIdentifier.setReferenceResolver()" +
            " with an existing ReferenceResolver."
)