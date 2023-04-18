import snappy
import numpy


class DummyTestOp:

    def __init__(self):
        # jpy = snappy.jpy
        # jpy.diag.flags = jpy.diag.F_ALL
        self.first_band = None
        self.second_band = None

    def initialize(self, context):
        source_product = context.getSourceProduct('source')

        width = source_product.getSceneRasterWidth()
        height = source_product.getSceneRasterHeight()

        self.first_band = source_product.getBandAt(0)
        self.second_band = source_product.getBandAt(1)

        dummyProduct = snappy.Product('py_dummy', 'forTesting', width, height)
        snappy.ProductUtils.copyGeoCoding(source_product, dummyProduct)
        self.mul_band = dummyProduct.addBand('mul', snappy.ProductData.TYPE_FLOAT32)
        self.div_band = dummyProduct.addBand('div', snappy.ProductData.TYPE_FLOAT32)
        self.sub_band = dummyProduct.addBand('sub', snappy.ProductData.TYPE_FLOAT32)
        self.add_band = dummyProduct.addBand('add', snappy.ProductData.TYPE_FLOAT32)

        context.setTargetProduct(dummyProduct)

    def computeTileStack(self, context, target_tiles, target_rectangle):
        first_tile = context.getSourceTile(self.first_band, target_rectangle)
        second_tile = context.getSourceTile(self.second_band, target_rectangle)

        first_samples = first_tile.getSamplesFloat()
        second_samples = second_tile.getSamplesFloat()

        first_data = numpy.array(first_samples, dtype=numpy.float32)
        second_data = numpy.array(second_samples, dtype=numpy.float32)

        mul = first_data * second_data
        div = first_data / second_data
        sub = first_data - second_data
        add = first_data + second_data

        mul_tile = target_tiles.get(self.mul_band)
        div_tile = target_tiles.get(self.div_band)
        sub_tile = target_tiles.get(self.sub_band)
        add_tile = target_tiles.get(self.add_band)
        mul_tile.setSamples(mul)
        div_tile.setSamples(div)
        sub_tile.setSamples(sub)
        add_tile.setSamples(add)

    def dispose(self, context):
        pass
