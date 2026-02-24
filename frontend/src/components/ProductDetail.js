import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Container, Row, Col, Card, Button, Spinner, Alert } from 'react-bootstrap';
import { toast } from 'react-toastify';

const ProductDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchProduct();
  }, [id]);

  const fetchProduct = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const response = await fetch(`/api/products/${id}`);
      if (!response.ok) {
        throw new Error('Product not found');
      }
      
      const data = await response.json();
      setProduct(data);
    } catch (err) {
      setError(err.message);
      toast.error('Failed to load product details');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <Container className="text-center py-5">
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading product...</span>
        </Spinner>
      </Container>
    );
  }

  if (error || !product) {
    return (
      <Container className="py-5">
        <Alert variant="danger">
          {error || 'Product not found'}
        </Alert>
        <Button variant="primary" onClick={() => navigate('/')}>
          Back to Products
        </Button>
      </Container>
    );
  }

  return (
    <Container className="py-4">
      <Button 
        variant="outline-secondary" 
        onClick={() => navigate('/')}
        className="mb-4"
      >
        ← Back to Products
      </Button>

      <Row>
        <Col lg={6}>
          {product.imageUrl ? (
            <Card>
              <Card.Img
                variant="top"
                src={product.imageUrl}
                alt={product.name}
                style={{ height: '400px', objectFit: 'cover' }}
              />
            </Card>
          ) : (
            <Card className="d-flex align-items-center justify-content-center" style={{ height: '400px' }}>
              <span className="text-muted fs-4">No Image Available</span>
            </Card>
          )}
        </Col>
        
        <Col lg={6}>
          <Card>
            <Card.Body>
              <div className="mb-2">
                <span className="badge bg-secondary">{product.categoryName}</span>
              </div>
              
              <h1 className="h2 mb-3">{product.name}</h1>
              
              <div className="price mb-3">
                <span className="h3 text-success">${product.price}</span>
              </div>
              
              <div className="mb-3">
                <h5>Description</h5>
                <p className="text-muted">
                  {product.description || 'No description available.'}
                </p>
              </div>
              
              <div className="mb-3">
                <h5>Stock Information</h5>
                <p className={product.stockQuantity > 0 ? 'text-success' : 'text-danger'}>
                  {product.stockQuantity > 0 
                    ? `${product.stockQuantity} units available` 
                    : 'Out of stock'
                  }
                </p>
              </div>
              
              <div className="mb-3">
                <h5>Product Details</h5>
                <ul className="list-unstyled">
                  <li><strong>Product ID:</strong> {product.id}</li>
                  <li><strong>Added:</strong> {new Date(product.createdAt).toLocaleDateString()}</li>
                  <li><strong>Last Updated:</strong> {new Date(product.updatedAt).toLocaleDateString()}</li>
                  <li><strong>Status:</strong> 
                    <span className={`badge ${product.isActive ? 'bg-success' : 'bg-secondary'} ms-2`}>
                      {product.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </li>
                </ul>
              </div>
              
              <div className="d-grid gap-2">
                <Button 
                  variant="primary" 
                  size="lg"
                  disabled={product.stockQuantity === 0}
                >
                  {product.stockQuantity > 0 ? 'Add to Cart' : 'Out of Stock'}
                </Button>
                <Button variant="outline-secondary">
                  Add to Wishlist
                </Button>
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
};

export default ProductDetail;
